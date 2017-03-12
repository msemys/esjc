package com.github.msemys.esjc.projection;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

import static com.github.msemys.esjc.util.Strings.defaultIfEmpty;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Exception thrown if a projection operation fails.
 */
public class ProjectionException extends RuntimeException {

    /**
     * The HTTP status code returned by the server
     */
    public final int httpStatusCode;

    /**
     * Creates a new instance with the specified status code and error message.
     *
     * @param httpStatusCode HTTP status code.
     * @param message        error message.
     */
    public ProjectionException(int httpStatusCode, String message) {
        super(message);
        this.httpStatusCode = httpStatusCode;
    }

    /**
     * Creates a new instance from the specified HTTP request and response.
     *
     * @param request  HTTP request.
     * @param response HTTP response.
     */
    public ProjectionException(HttpRequest request, FullHttpResponse response) {
        this(response.getStatus().code(), String.format("Server returned %d (%s) for %s on %s",
            response.getStatus().code(),
            defaultIfEmpty(response.content().toString(UTF_8), response.getStatus().reasonPhrase()),
            request.getMethod().name(),
            request.getUri()));
    }

}
