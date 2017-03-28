package com.github.msemys.esjc.user;

import com.github.msemys.esjc.UserCredentials;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * User manager for managing users in the Event Store.
 * It is recommended that only one instance per application is created.
 */
public interface UserManager {

    /**
     * Enables a user using default user credentials.
     *
     * @param name the login name of the user.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #enable(String, UserCredentials)
     */
    default CompletableFuture<Void> enable(String name) {
        return enable(name, null);
    }

    /**
     * Enables a user.
     *
     * @param name            the login name of the user.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> enable(String name, UserCredentials userCredentials);

    /**
     * Disables a user using default user credentials.
     *
     * @param name the login name of the user.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #disable(String, UserCredentials)
     */
    default CompletableFuture<Void> disable(String name) {
        return disable(name, null);
    }

    /**
     * Disables a user.
     *
     * @param name            the login name of the user.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> disable(String name, UserCredentials userCredentials);

    /**
     * Deletes a user using default user credentials.
     *
     * @param name the login name of the user.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #delete(String, UserCredentials)
     */
    default CompletableFuture<Void> delete(String name) {
        return delete(name, null);
    }

    /**
     * Deletes a user.
     *
     * @param name            the login name of the user.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> delete(String name, UserCredentials userCredentials);

    /**
     * Finds all users using default user credentials.
     *
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException} or
     * {@link UserException} on exceptional completion.
     * @see #findAll(UserCredentials)
     */
    default CompletableFuture<List<User>> findAll() {
        return findAll(null);
    }

    /**
     * Finds all users.
     *
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException} or
     * {@link UserException} on exceptional completion.
     */
    CompletableFuture<List<User>> findAll(UserCredentials userCredentials);

    /**
     * Finds user by the specified login name using default user credentials.
     *
     * @param name the login name of the user.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException} or
     * {@link UserException} on exceptional completion.
     * @see #find(String, UserCredentials)
     */
    default CompletableFuture<User> find(String name) {
        return find(name, null);
    }

    /**
     * Finds user by the specified login name.
     *
     * @param name            the login name of the user.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException} or
     * {@link UserException} on exceptional completion.
     */
    CompletableFuture<User> find(String name, UserCredentials userCredentials);

    /**
     * Gets the current user using default user credentials.
     *
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException} or
     * {@link UserException} on exceptional completion.
     * @see #getCurrent(UserCredentials)
     */
    default CompletableFuture<User> getCurrent() {
        return getCurrent(null);
    }

    /**
     * Gets the current user.
     *
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException} or
     * {@link UserException} on exceptional completion.
     */
    CompletableFuture<User> getCurrent(UserCredentials userCredentials);

    /**
     * Creates a new user using default user credentials.
     *
     * @param name     the login name of the new user.
     * @param fullName the full name of the new user.
     * @param password the password of the new user.
     * @param groups   the groups the new user should become a member of.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #create(String, String, String, List, UserCredentials)
     */
    default CompletableFuture<Void> create(String name, String fullName, String password, List<String> groups) {
        return create(name, fullName, password, groups, null);
    }

    /**
     * Creates a new user.
     *
     * @param name            the login name of the new user.
     * @param fullName        the full name of the new user.
     * @param password        the password of the new user.
     * @param groups          the groups the new user should become a member of.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> create(String name, String fullName, String password, List<String> groups, UserCredentials userCredentials);

    /**
     * Updates an existing user using default user credentials.
     *
     * @param name     the login name of the user to update.
     * @param fullName the full name of the user being updated.
     * @param groups   the groups the updated user should be a member of.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException} or
     * {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #update(String, String, List, UserCredentials)
     */
    default CompletableFuture<Void> update(String name, String fullName, List<String> groups) {
        return update(name, fullName, groups, null);
    }

    /**
     * Updates an existing user.
     *
     * @param name            the login name of the user to update.
     * @param fullName        the full name of the user being updated.
     * @param groups          the groups the updated user should be a member of.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException} or
     * {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> update(String name, String fullName, List<String> groups, UserCredentials userCredentials);

    /**
     * Changes a users password using default user credentials.
     *
     * @param name        the login name of the user who's password should be changed.
     * @param oldPassword the old password of the user.
     * @param newPassword the new password of the user.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #changePassword(String, String, String, UserCredentials)
     */
    default CompletableFuture<Void> changePassword(String name, String oldPassword, String newPassword) {
        return changePassword(name, oldPassword, newPassword, null);
    }

    /**
     * Changes a users password.
     *
     * @param name            the login name of the user who's password should be changed.
     * @param oldPassword     the old password of the user.
     * @param newPassword     the new password of the user.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> changePassword(String name, String oldPassword, String newPassword, UserCredentials userCredentials);

    /**
     * Resets a users password using default user credentials.
     *
     * @param name        the login name of the user who's password should be reset.
     * @param newPassword the new password of the user.
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     * @see #resetPassword(String, String, UserCredentials)
     */
    default CompletableFuture<Void> resetPassword(String name, String newPassword) {
        return resetPassword(name, newPassword, null);
    }

    /**
     * Resets a users password.
     *
     * @param name            the login name of the user who's password should be reset.
     * @param newPassword     the new password of the user.
     * @param userCredentials user credentials to be used for this operation (use {@code null} for default user credentials).
     * @return a {@code CompletableFuture} representing the result of this operation. The future's methods
     * {@code get} and {@code join} can throw an exception with cause {@link UserNotFoundException},
     * {@link UserConflictException} or {@link UserException} on exceptional completion.
     * In case of successful completion, the future's methods {@code get} and {@code join} returns {@code null}.
     */
    CompletableFuture<Void> resetPassword(String name, String newPassword, UserCredentials userCredentials);

    /**
     * Shut down this user manager.
     */
    void shutdown();

}
