package com.github.msemys.esjc.http;

import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.http.handler.HttpResponseHandler;
import com.github.msemys.esjc.util.concurrent.ResettableLatch;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Base64;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.msemys.esjc.util.Numbers.isPositive;
import static com.github.msemys.esjc.util.Preconditions.*;
import static com.github.msemys.esjc.util.Strings.*;
import static io.netty.buffer.Unpooled.copiedBuffer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * HTTP client without pipelining support
 */
public class HttpClient implements AutoCloseable {
    private final EventLoopGroup group = new NioEventLoopGroup(0, new DefaultThreadFactory("es-http"));
    private final Bootstrap bootstrap;
    private final String host;
    private final boolean acceptGzip;
    private final long operationTimeoutMillis;

    private final Queue<HttpOperation> queue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean isProcessing = new AtomicBoolean();
    private final ResettableLatch received = new ResettableLatch(false);

    private volatile Channel channel;

    private HttpClient(Builder builder) {
        host = builder.address.getHostString();
        acceptGzip = builder.acceptGzip;
        operationTimeoutMillis = builder.operationTimeout.toMillis();

        bootstrap = new Bootstrap()
            .remoteAddress(builder.address)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) builder.connectTimeout.toMillis())
            .group(group)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline pipeline = ch.pipeline();

                    pipeline.addLast("http-codec", new HttpClientCodec());
                    if (acceptGzip) {
                        pipeline.addLast("content-decompressor", new HttpContentDecompressor());
                    }
                    pipeline.addLast("object-aggregator", new HttpObjectAggregator(builder.maxContentLength));
                    pipeline.addLast("logger", new LoggingHandler(HttpClient.class, LogLevel.TRACE));
                    pipeline.addLast("response-handler", new HttpResponseHandler());
                }
            });
    }

    public CompletableFuture<FullHttpResponse> send(HttpRequest request) {
        checkNotNull(request, "request is null");
        checkState(isRunning(), "HTTP client is closed");

        request.headers().set(HttpHeaders.Names.HOST, host);

        if (acceptGzip) {
            request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);
        }

        CompletableFuture<FullHttpResponse> response = new CompletableFuture<>();
        enqueue(new HttpOperation(request, response));

        return response;
    }

    private void enqueue(HttpOperation operation) {
        checkNotNull(operation, "operation is null");

        queue.offer(operation);

        if (isProcessing.compareAndSet(false, true)) {
            executor().execute(this::processQueue);
        }
    }

    private void processQueue() {
        do {
            HttpOperation operation;

            while ((operation = queue.poll()) != null) {
                if (channel == null || !channel.isActive()) {
                    try {
                        ChannelFuture future = bootstrap.connect().await();

                        if (future.isSuccess()) {
                            channel = future.channel();
                        } else {
                            operation.response.completeExceptionally(future.cause());
                            continue;
                        }
                    } catch (Exception e) {
                        operation.response.completeExceptionally(e);
                        continue;
                    }
                }

                write(operation);
            }

            isProcessing.set(false);
        } while (isRunning() && !queue.isEmpty() && isProcessing.compareAndSet(false, true));
    }

    private void write(HttpOperation operation) {
        received.reset();

        operation.response.whenComplete((r, t) -> received.release());

        HttpResponseHandler responseHandler = channel.pipeline().get(HttpResponseHandler.class);
        responseHandler.pendingResponse = operation.response;

        try {
            channel.writeAndFlush(operation.request).await();

            if (!received.await(operationTimeoutMillis, MILLISECONDS)) {
                channel.close().awaitUninterruptibly();

                String message = String.format("%s %s request never got response from server.",
                    operation.request.getMethod().name(),
                    operation.request.getUri());

                operation.response.completeExceptionally(new TimeoutException(message));
            }
        } catch (InterruptedException e) {
            operation.response.completeExceptionally(e);
        } finally {
            responseHandler.pendingResponse = null;
            channel.closeFuture().awaitUninterruptibly();
        }
    }

    private boolean isRunning() {
        return !group.isShuttingDown();
    }

    @Override
    public void close() {
        group.shutdownGracefully();

        HttpOperation operation;
        while ((operation = queue.poll()) != null) {
            operation.response.completeExceptionally(new HttpClientException("Client closed"));
        }
    }

    private Executor executor() {
        return group;
    }

    private static void addAuthorizationHeader(FullHttpRequest request, UserCredentials userCredentials) {
        checkNotNull(request, "request is null");
        checkNotNull(userCredentials, "userCredentials is null");

        byte[] encodedCredentials = Base64.getEncoder().encode(toBytes(userCredentials.username + ":" + userCredentials.password));
        request.headers().add(HttpHeaders.Names.AUTHORIZATION, "Basic " + newString(encodedCredentials));
    }

    public static FullHttpRequest newRequest(HttpMethod method, String uri, UserCredentials userCredentials) {
        checkNotNull(method, "method is null");
        checkArgument(!isNullOrEmpty(uri), "uri is null or empty");

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri);
        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);

        if (userCredentials != null) {
            addAuthorizationHeader(request, userCredentials);
        }

        return request;
    }

    public static FullHttpRequest newRequest(HttpMethod method, String uri, String body, String contentType, UserCredentials userCredentials) {
        checkNotNull(method, "method is null");
        checkArgument(!isNullOrEmpty(uri), "uri is null or empty");
        checkNotNull(body, "body is null");
        checkArgument(!isNullOrEmpty(contentType), "contentType is null or empty");

        ByteBuf data = copiedBuffer(body, UTF_8);

        FullHttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri, data);
        request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
        request.headers().set(HttpHeaders.Names.CONTENT_TYPE, contentType);
        request.headers().set(HttpHeaders.Names.CONTENT_LENGTH, data.readableBytes());

        if (userCredentials != null) {
            addAuthorizationHeader(request, userCredentials);
        }

        return request;
    }

    /**
     * Creates a new HTTP client builder.
     *
     * @return HTTP client builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * HTTP client builder.
     */
    public static class Builder {
        private InetSocketAddress address;
        private Duration connectTimeout;
        private Duration operationTimeout;
        private Boolean acceptGzip;
        private Integer maxContentLength;

        /**
         * Sets server address.
         *
         * @param host the host name.
         * @param port the port number.
         * @return the builder reference
         */
        public Builder address(String host, int port) {
            return address(new InetSocketAddress(host, port));
        }

        /**
         * Sets server address.
         *
         * @param address the server address.
         * @return the builder reference
         */
        public Builder address(InetSocketAddress address) {
            this.address = address;
            return this;
        }

        /**
         * Sets connection establishment timeout (by default, 10 seconds).
         *
         * @param connectTimeout connection establishment timeout.
         * @return the builder reference
         */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * Sets the amount of time before an operation is considered to have timed out (by default, 7 seconds).
         *
         * @param operationTimeout the amount of time before an operation is considered to have timed out.
         * @return the builder reference
         */
        public Builder operationTimeout(Duration operationTimeout) {
            this.operationTimeout = operationTimeout;
            return this;
        }

        /**
         * Specifies whether or not the client accepts compressed content (by default, does not accept compressed content).
         *
         * @param acceptGzip {@code true} to accept.
         * @return the builder reference
         */
        public Builder acceptGzip(boolean acceptGzip) {
            this.acceptGzip = acceptGzip;
            return this;
        }

        /**
         * Sets the maximum length of the response content in bytes (by default, 128 megabytes).
         *
         * @param maxContentLength the maximum length of the response content in bytes.
         * @return the builder reference
         */
        public Builder maxContentLength(int maxContentLength) {
            this.maxContentLength = maxContentLength;
            return this;
        }

        /**
         * Builds a HTTP client.
         *
         * @return HTTP client
         */
        public HttpClient build() {
            checkNotNull(address, "address is null");

            if (connectTimeout == null) {
                connectTimeout = Duration.ofSeconds(10);
            }

            if (operationTimeout == null) {
                operationTimeout = Duration.ofSeconds(7);
            }

            if (acceptGzip == null) {
                acceptGzip = false;
            }

            if (maxContentLength == null) {
                maxContentLength = 128 * 1024 * 1024;
            } else {
                checkArgument(isPositive(maxContentLength), "maxContentLength should be positive");
            }

            return new HttpClient(this);
        }
    }

    private static class HttpOperation {
        final HttpRequest request;
        final CompletableFuture<FullHttpResponse> response;

        HttpOperation(HttpRequest request, CompletableFuture<FullHttpResponse> response) {
            checkNotNull(request, "request is null");
            checkNotNull(response, "response is null");
            this.request = request;
            this.response = response;
        }
    }

}
