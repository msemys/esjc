package com.github.msemys.esjc.user.dto;

import java.util.List;

public class UserCreateDetails {
    public final String loginName;
    public final String fullName;
    public final String password;
    public final List<String> groups;

    public UserCreateDetails(String loginName, String fullName, String password, List<String> groups) {
        this.loginName = loginName;
        this.fullName = fullName;
        this.password = password;
        this.groups = groups;
    }
}
