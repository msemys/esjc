package com.github.msemys.esjc.http;

import io.netty.handler.codec.http.HttpRequest;

/**
 * Exception thrown if HTTP operation times out.
 */
public class HttpOperationTimedOutException extends HttpClientException {

    public HttpOperationTimedOutException(String message) {
        super(message);
    }

    public HttpOperationTimedOutException(HttpRequest request) {
        super(String.format("%s %s request never got response from server.", request.getMethod().name(), request.getUri()));
    }

}
