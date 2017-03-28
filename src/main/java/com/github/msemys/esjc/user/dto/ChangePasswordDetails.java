package com.github.msemys.esjc.user.dto;

public class ChangePasswordDetails {
    public final String currentPassword;
    public final String newPassword;

    public ChangePasswordDetails(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }
}
