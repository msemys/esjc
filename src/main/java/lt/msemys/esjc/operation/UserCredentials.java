package lt.msemys.esjc.operation;

import static lt.msemys.esjc.util.Preconditions.checkNotNull;

/**
 * @see <a href="https://github.com/EventStore/EventStore/blob/dev/src/EventStore.ClientAPI/SystemData/UserCredentials.cs">EventStore.ClientAPI/SystemData/UserCredentials.cs</a>
 */
public class UserCredentials {

    public final String username;
    public final String password;

    public UserCredentials(String username, String password) {
        checkNotNull(username, "User name is not specified.");
        checkNotNull(password, "Password is not specified.");
        this.username = username;
        this.password = password;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UserCredentials{");
        sb.append("username='").append(username).append('\'');
        sb.append(", password='****").append('\'');
        sb.append('}');
        return sb.toString();
    }
}
