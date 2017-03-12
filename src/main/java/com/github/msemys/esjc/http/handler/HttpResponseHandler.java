package com.github.msemys.esjc.http.handler;

import com.github.msemys.esjc.http.HttpClientException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class HttpResponseHandler extends SimpleChannelInboundHandler<FullHttpResponse> {
    private static final Logger logger = LoggerFactory.getLogger(HttpResponseHandler.class);

    public volatile CompletableFuture<FullHttpResponse> pendingResponse;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
        if (pendingResponse != null) {
            pendingResponse.complete(msg);
        } else {
            logger.warn("Unexpected HTTP response received: {}", msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (pendingResponse != null) {
            pendingResponse.completeExceptionally(cause);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (pendingResponse != null) {
            pendingResponse.completeExceptionally(new HttpClientException("Connection closed"));
        }
        ctx.fireChannelInactive();
    }

}
