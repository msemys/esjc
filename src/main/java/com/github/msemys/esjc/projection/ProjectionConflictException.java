package com.github.msemys.esjc.projection;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Exception thrown if a projection operation fails with status code {@code 409} (Conflict).
 */
public class ProjectionConflictException extends ProjectionException {

    /**
     * Creates a new instance with the specified status code and error message.
     *
     * @param httpStatusCode HTTP status code.
     * @param message        error message.
     */
    public ProjectionConflictException(int httpStatusCode, String message) {
        super(httpStatusCode, message);
    }

    /**
     * Creates a new instance from the specified HTTP request and response.
     *
     * @param request  HTTP request.
     * @param response HTTP response.
     */
    public ProjectionConflictException(HttpRequest request, FullHttpResponse response) {
        super(request, response);
    }
}
