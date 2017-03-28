package com.github.msemys.esjc.user;

import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Exception thrown if a user operation fails with status code {@code 409} (Conflict).
 */
public class UserConflictException extends UserException {

    /**
     * Creates a new instance with the specified status code and error message.
     *
     * @param httpStatusCode HTTP status code.
     * @param message        error message.
     */
    public UserConflictException(int httpStatusCode, String message) {
        super(httpStatusCode, message);
    }

    /**
     * Creates a new instance from the specified HTTP request and response.
     *
     * @param request  HTTP request.
     * @param response HTTP response.
     */
    public UserConflictException(HttpRequest request, FullHttpResponse response) {
        super(request, response);
    }

}
