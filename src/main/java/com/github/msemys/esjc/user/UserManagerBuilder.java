package com.github.msemys.esjc.user;

import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.http.HttpClient;

import java.net.InetSocketAddress;
import java.time.Duration;

/**
 * User manager builder.
 */
public class UserManagerBuilder {
    private final HttpClient.Builder httpClientBuilder = HttpClient.newBuilder();
    private UserCredentials userCredentials;

    private UserManagerBuilder() {
    }

    /**
     * Creates a new user manager builder.
     *
     * @return user manager builder.
     */
    public static UserManagerBuilder newBuilder() {
        return new UserManagerBuilder();
    }

    /**
     * Sets server address (HTTP endpoint).
     *
     * @param host the host name.
     * @param port the HTTP port number.
     * @return the builder reference
     */
    public UserManagerBuilder address(String host, int port) {
        httpClientBuilder.address(host, port);
        return this;
    }

    /**
     * Sets server address (HTTP endpoint).
     *
     * @param address the server address.
     * @return the builder reference
     */
    public UserManagerBuilder address(InetSocketAddress address) {
        httpClientBuilder.address(address);
        return this;
    }

    /**
     * Sets connection establishment timeout (by default, 10 seconds).
     *
     * @param connectTimeout connection establishment timeout.
     * @return the builder reference
     */
    public UserManagerBuilder connectTimeout(Duration connectTimeout) {
        httpClientBuilder.connectTimeout(connectTimeout);
        return this;
    }

    /**
     * Sets the amount of time before an operation is considered to have timed out (by default, 7 seconds).
     *
     * @param operationTimeout the amount of time before an operation is considered to have timed out.
     * @return the builder reference
     */
    public UserManagerBuilder operationTimeout(Duration operationTimeout) {
        httpClientBuilder.operationTimeout(operationTimeout);
        return this;
    }

    /**
     * Sets the maximum length of the response content in bytes (by default, 128 megabytes).
     *
     * @param maxContentLength the maximum length of the response content in bytes.
     * @return the builder reference
     */
    public UserManagerBuilder maxContentLength(int maxContentLength) {
        httpClientBuilder.maxContentLength(maxContentLength);
        return this;
    }

    /**
     * Sets the default user credentials to be used for operations.
     * If user credentials are not given for an operation, these credentials will be used.
     *
     * @param userCredentials user credentials.
     * @return the builder reference
     */
    public UserManagerBuilder userCredentials(UserCredentials userCredentials) {
        this.userCredentials = userCredentials;
        return this;
    }

    /**
     * Sets the default user credentials to be used for operations.
     * If user credentials are not given for an operation, these credentials will be used.
     *
     * @param username user name.
     * @param password user password.
     * @return the builder reference
     */
    public UserManagerBuilder userCredentials(String username, String password) {
        return userCredentials(new UserCredentials(username, password));
    }

    /**
     * Sets no default user credentials for operations.
     *
     * @return the builder reference
     * @see #userCredentials(String, String)
     * @see #userCredentials(UserCredentials)
     */
    public UserManagerBuilder noUserCredentials() {
        return userCredentials(null);
    }

    /**
     * Builds a user manager.
     *
     * @return user manager
     */
    public UserManager build() {
        return new UserManagerHttp(httpClientBuilder.build(), userCredentials);
    }

}
