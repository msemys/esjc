package com.github.msemys.esjc.user;

import com.github.msemys.esjc.UserCredentials;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static com.github.msemys.esjc.util.Threads.sleepUninterruptibly;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.*;

public class ITUserManager {

    private UserManager userManager;

    @Before
    public void setUp() throws Exception {
        userManager = UserManagerBuilder.newBuilder()
            .address("127.0.0.1", 2113)
            .userCredentials("admin", "changeit")
            .operationTimeout(Duration.ofSeconds(20))
            .build();
    }

    @After
    public void tearDown() throws Exception {
        userManager.shutdown();
    }

    @Test
    public void disableFailsWhenUserDoesNotExists() {
        try {
            userManager.disable(UUID.randomUUID().toString()).join();
            fail("should fail with 'UserNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(UserNotFoundException.class));
        }
    }

    @Test
    public void enableFailsWhenUserDoesNotExists() {
        try {
            userManager.enable(UUID.randomUUID().toString()).join();
            fail("should fail with 'UserNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(UserNotFoundException.class));
        }
    }

    @Test
    public void disablesAndEnablesUser() {
        final String username = UUID.randomUUID().toString();

        userManager.create(username, username + "-full-name", "password", asList("foo", "bar")).join();
        assertFalse(userManager.find(username).join().disabled);

        userManager.disable(username).join();
        assertTrue(userManager.find(username).join().disabled);

        userManager.enable(username).join();
        assertFalse(userManager.find(username).join().disabled);
    }

    @Test
    public void deleteFailsWhenUserDoesNotExists() {
        try {
            userManager.delete(UUID.randomUUID().toString()).join();
            fail("should fail with 'UserNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(UserNotFoundException.class));
        }
    }

    @Test
    public void deletesUser() {
        final String username = UUID.randomUUID().toString();

        assertUserDoesNotExists(username);

        userManager.create(username, username + "-full-name", "password", asList("foo", "bar")).join();
        assertEquals(username, userManager.find(username).join().loginName);

        userManager.delete(username).join();
        assertUserDoesNotExists(username);
    }

    @Test
    public void findsAllUsers() {
        List<User> users = userManager.findAll().join();

        assertFalse(users.isEmpty());

        users.forEach(u -> assertFalse(u.loginName.isEmpty()));
    }

    @Test
    public void findFailsWhenUserDoesNotExists() {
        try {
            userManager.find(UUID.randomUUID().toString()).join();
            fail("should fail with 'UserNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(UserNotFoundException.class));
        }
    }

    @Test
    public void findsUser() {
        final String username = UUID.randomUUID().toString();
        final String fullName = username + "-full-name";
        final List<String> groups = asList("foo", "bar", "baz");

        userManager.create(username, fullName, "password", groups).join();

        User user = userManager.find(username).join();

        assertEquals(username, user.loginName);
        assertEquals(fullName, user.fullName);
        assertEquals(groups, user.groups);
        assertFalse(user.disabled);
        assertNull(user.dateLastUpdated);
    }

    @Test
    public void getsCurrentUser() {
        User user = userManager.getCurrent().join();

        assertEquals("admin", user.loginName);
        assertEquals("Event Store Administrator", user.fullName);
        assertEquals(asList("$admins"), user.groups);
        assertFalse(user.disabled);
        assertNotNull(user.dateLastUpdated);
    }

    @Test
    public void createFailsWhenUserAlreadyExists() {
        try {
            userManager.create("admin", "---", "---", emptyList()).join();
            fail("should fail with 'UserConflictException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(UserConflictException.class));
        }
    }

    @Test
    public void createsNewUser() {
        final String username = UUID.randomUUID().toString();
        final String fullName = username + "-full-name";
        final List<String> groups = emptyList();

        assertUserDoesNotExists(username);

        userManager.create(username, fullName, "password", groups).join();

        User user = userManager.find(username).join();

        assertEquals(username, user.loginName);
        assertEquals(fullName, user.fullName);
        assertEquals(groups, user.groups);
        assertFalse(user.disabled);
        assertNull(user.dateLastUpdated);
    }

    @Test
    public void updateFailsWhenUserDoesNotExists() {
        try {
            userManager.update(UUID.randomUUID().toString(), "---", emptyList()).join();
            fail("should fail with 'UserNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(UserNotFoundException.class));
        }
    }

    @Test
    public void updatesUser() {
        final String username = UUID.randomUUID().toString();
        final String fullName = username + "-full-name";
        final List<String> groups = emptyList();

        assertUserDoesNotExists(username);

        userManager.create(username, fullName, "password", groups).join();

        User user = userManager.find(username).join();

        assertEquals(username, user.loginName);
        assertEquals(fullName, user.fullName);
        assertEquals(groups, user.groups);
        assertFalse(user.disabled);
        assertNull(user.dateLastUpdated);

        final String newFullName = username + "-NEW-FULL-NAME";
        final List<String> newGroups = asList("foo", "bar");

        userManager.update(username, newFullName, newGroups).join();

        User updatedUser = userManager.find(username).join();

        assertEquals(username, updatedUser.loginName);
        assertEquals(newFullName, updatedUser.fullName);
        assertEquals(newGroups, updatedUser.groups);
        assertFalse(updatedUser.disabled);
        assertNull(updatedUser.dateLastUpdated);
    }

    @Test
    public void changePasswordFailsWhenUserDoesNotExists() {
        try {
            userManager.changePassword(UUID.randomUUID().toString(), "---", "---").join();
            fail("should fail with 'UserNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(UserNotFoundException.class));
        }
    }

    @Test
    public void changesPassword() {
        final String username = UUID.randomUUID().toString();
        final String password = "password";
        final String newPassword = "newPassword";

        userManager.create(username, "---", password, asList("$admins")).join();
        assertFalse(userManager.findAll(new UserCredentials(username, password)).join().isEmpty());

        userManager.changePassword(username, password, newPassword).join();

        sleepUninterruptibly(3000);

        assertFalse(userManager.findAll(new UserCredentials(username, newPassword)).join().isEmpty());
    }

    @Test
    public void resetPasswordFailsWhenUserDoesNotExists() {
        try {
            userManager.resetPassword(UUID.randomUUID().toString(), "---").join();
            fail("should fail with 'UserNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(UserNotFoundException.class));
        }
    }

    @Test
    public void resetsPassword() {
        final String username = UUID.randomUUID().toString();
        final String password = "password";
        final String newPassword = "newPassword";

        userManager.create(username, "---", password, asList("$admins")).join();
        assertFalse(userManager.findAll(new UserCredentials(username, password)).join().isEmpty());

        userManager.resetPassword(username, newPassword).join();

        sleepUninterruptibly(3000);

        assertFalse(userManager.findAll(new UserCredentials(username, newPassword)).join().isEmpty());
    }

    private void assertUserDoesNotExists(String username) {
        try {
            userManager.find(username).join();
            fail("should fail with 'UserNotFoundException'");
        } catch (Exception e) {
            assertThat(e.getCause(), instanceOf(UserNotFoundException.class));
        }
    }

}
