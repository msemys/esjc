package com.github.msemys.esjc.user;

import com.github.msemys.esjc.UserCredentials;
import com.github.msemys.esjc.http.HttpClient;
import com.github.msemys.esjc.user.dto.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.github.msemys.esjc.http.HttpClient.newRequest;
import static com.github.msemys.esjc.util.Preconditions.checkArgument;
import static com.github.msemys.esjc.util.Preconditions.checkNotNull;
import static com.github.msemys.esjc.util.Strings.EMPTY;
import static com.github.msemys.esjc.util.Strings.isNullOrEmpty;
import static io.netty.handler.codec.http.HttpHeaders.Values.APPLICATION_JSON;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

/**
 * User manager for managing users in the Event Store, that
 * communicates with server over the RESTful API.
 */
public class UserManagerHttp implements UserManager {
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(OffsetDateTime.class,
            (JsonDeserializer<OffsetDateTime>) (json, type, ctx) -> OffsetDateTime.parse(json.getAsJsonPrimitive().getAsString()))
        .create();

    private final HttpClient client;
    private final UserCredentials userCredentials;

    protected UserManagerHttp(HttpClient client, UserCredentials userCredentials) {
        checkNotNull(client, "client is null");
        this.client = client;
        this.userCredentials = userCredentials;
    }

    @Override
    public CompletableFuture<Void> enable(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return post(userUri(name) + "/command/enable", EMPTY, userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<Void> disable(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return post(userUri(name) + "/command/disable", EMPTY, userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<Void> delete(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return delete(userUri(name), userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<List<User>> findAll(UserCredentials userCredentials) {
        return get("/users/", userCredentials, HttpResponseStatus.OK).thenApply(UserManagerHttp::asUserList);
    }

    @Override
    public CompletableFuture<User> find(String name, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");

        return get(userUri(name), userCredentials, HttpResponseStatus.OK).thenApply(UserManagerHttp::asUser);
    }

    @Override
    public CompletableFuture<User> getCurrent(UserCredentials userCredentials) {
        return get("/users/$current", userCredentials, HttpResponseStatus.OK).thenApply(UserManagerHttp::asUser);
    }

    @Override
    public CompletableFuture<Void> create(String name, String fullName, String password, List<String> groups, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");
        checkArgument(!isNullOrEmpty(fullName), "fullName is null or empty");
        checkArgument(!isNullOrEmpty(password), "password is null or empty");
        checkNotNull(groups, "groups is null");

        return post("/users/", gson.toJson(new UserCreateDetails(name, fullName, password, groups)), userCredentials, HttpResponseStatus.CREATED);
    }

    @Override
    public CompletableFuture<Void> update(String name, String fullName, List<String> groups, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");
        checkArgument(!isNullOrEmpty(fullName), "fullName is null or empty");
        checkNotNull(groups, "groups is null");

        return put(userUri(name), gson.toJson(new UserUpdateDetails(fullName, groups)), userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<Void> changePassword(String name, String oldPassword, String newPassword, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");
        checkArgument(!isNullOrEmpty(oldPassword), "oldPassword is null or empty");
        checkArgument(!isNullOrEmpty(newPassword), "newPassword is null or empty");

        return post(userUri(name) + "/command/change-password", gson.toJson(new ChangePasswordDetails(oldPassword, newPassword)), userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public CompletableFuture<Void> resetPassword(String name, String newPassword, UserCredentials userCredentials) {
        checkArgument(!isNullOrEmpty(name), "name is null or empty");
        checkArgument(!isNullOrEmpty(newPassword), "newPassword is null or empty");

        return post(userUri(name) + "/command/reset-password", gson.toJson(new ResetPasswordDetails(newPassword)), userCredentials, HttpResponseStatus.OK);
    }

    @Override
    public void shutdown() {
        client.close();
    }

    private CompletableFuture<String> get(String uri, UserCredentials userCredentials, HttpResponseStatus expectedStatus) {
        FullHttpRequest request = newRequest(HttpMethod.GET, uri, defaultOr(userCredentials));

        return client.send(request).thenApply(response -> {
            if (response.status().code() == expectedStatus.code()) {
                return response.content().toString(UTF_8);
            } else if (response.status().code() == HttpResponseStatus.NOT_FOUND.code()) {
                throw new UserNotFoundException(request, response);
            } else {
                throw new UserException(request, response);
            }
        });
    }

    private CompletableFuture<Void> delete(String uri, UserCredentials userCredentials, HttpResponseStatus expectedStatus) {
        FullHttpRequest request = newRequest(HttpMethod.DELETE, uri, defaultOr(userCredentials));

        return client.send(request).thenAccept(response -> {
            if (response.status().code() == HttpResponseStatus.NOT_FOUND.code()) {
                throw new UserNotFoundException(request, response);
            } else if (response.status().code() != expectedStatus.code()) {
                throw new UserException(request, response);
            }
        });
    }

    private CompletableFuture<Void> put(String uri, String content, UserCredentials userCredentials, HttpResponseStatus expectedStatus) {
        FullHttpRequest request = newRequest(HttpMethod.PUT, uri, content, APPLICATION_JSON, defaultOr(userCredentials));

        return client.send(request).thenAccept(response -> {
            if (response.status().code() == HttpResponseStatus.NOT_FOUND.code()) {
                throw new UserNotFoundException(request, response);
            } else if (response.status().code() != expectedStatus.code()) {
                throw new UserException(request, response);
            }
        });
    }

    private CompletableFuture<Void> post(String uri, String content, UserCredentials userCredentials, HttpResponseStatus expectedStatus) {
        FullHttpRequest request = newRequest(HttpMethod.POST, uri, content, APPLICATION_JSON, defaultOr(userCredentials));

        return client.send(request).thenAccept(response -> {
            if (response.status().code() == HttpResponseStatus.NOT_FOUND.code()) {
                throw new UserNotFoundException(request, response);
            } else if (response.status().code() == HttpResponseStatus.CONFLICT.code()) {
                throw new UserConflictException(request, response);
            } else if (response.status().code() != expectedStatus.code()) {
                throw new UserException(request, response);
            }
        });
    }

    private UserCredentials defaultOr(UserCredentials userCredentials) {
        return (userCredentials == null) ? this.userCredentials : userCredentials;
    }

    private static List<User> asUserList(String json) {
        if (isNullOrEmpty(json)) {
            return emptyList();
        } else {
            UsersHolder holder = gson.fromJson(json, UsersHolder.class);
            return (holder.data != null) ? holder.data : emptyList();
        }
    }

    private static User asUser(String json) {
        return isNullOrEmpty(json) ? null : gson.fromJson(json, UserHolder.class).data;
    }

    private static String userUri(String name) {
        return "/users/" + name.trim();
    }

}
