package com.github.msemys.esjc.http;

import io.netty.handler.codec.http.HttpRequest;

/**
 * Exception thrown if HTTP operation times out.
 */
public class HttpOperationTimeoutException extends HttpClientException {

    public HttpOperationTimeoutException(String message) {
        super(message);
    }

    public HttpOperationTimeoutException(HttpRequest request) {
        super(String.format("%s %s request never got response from server.", request.method().name(), request.uri()));
    }

}
