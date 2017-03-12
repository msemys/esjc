package com.github.msemys.esjc.http;

/**
 * HTTP client exception.
 */
public class HttpClientException extends RuntimeException {

    /**
     * Creates a new instance with the specified error message.
     *
     * @param message error message.
     */
    public HttpClientException(String message) {
        super(message);
    }

    /**
     * Creates a new instance with the specified error message and cause.
     *
     * @param message error message.
     * @param cause   the cause.
     */
    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
