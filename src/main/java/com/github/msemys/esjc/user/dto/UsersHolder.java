package com.github.msemys.esjc.user.dto;

import com.github.msemys.esjc.user.User;

import java.util.List;

public class UsersHolder {
    public final List<User> data;

    public UsersHolder(List<User> data) {
        this.data = data;
    }
}
