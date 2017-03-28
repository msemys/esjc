package com.github.msemys.esjc.user;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Represents the details for a user.
 */
public class User {

    /**
     * The login name of the user.
     */
    public final String loginName;

    /**
     * The full name of the user.
     */
    public final String fullName;

    /**
     * The groups the user is a member of.
     */
    public final List<String> groups;

    /**
     * The date/time the user was updated in UTC format.
     */
    public final OffsetDateTime dateLastUpdated;

    /**
     * Whether or not the user is disabled.
     */
    public final boolean disabled;

    /**
     * List of hypermedia links describing actions allowed on user resource.
     */
    public final List<RelLink> links;

    public User(String loginName,
                String fullName,
                List<String> groups,
                OffsetDateTime dateLastUpdated,
                boolean disabled,
                List<RelLink> links) {
        this.loginName = loginName;
        this.fullName = fullName;
        this.groups = groups;
        this.dateLastUpdated = dateLastUpdated;
        this.disabled = disabled;
        this.links = links;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("User{");
        sb.append("loginName='").append(loginName).append('\'');
        sb.append(", fullName='").append(fullName).append('\'');
        sb.append(", groups=").append(groups);
        sb.append(", dateLastUpdated=").append(dateLastUpdated);
        sb.append(", disabled=").append(disabled);
        sb.append(", links=").append(links);
        sb.append('}');
        return sb.toString();
    }

}
