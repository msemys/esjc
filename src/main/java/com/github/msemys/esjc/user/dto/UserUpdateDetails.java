package com.github.msemys.esjc.user.dto;

import java.util.List;

public class UserUpdateDetails {
    public final String fullName;
    public final List<String> groups;

    public UserUpdateDetails(String fullName, List<String> groups) {
        this.fullName = fullName;
        this.groups = groups;
    }
}
