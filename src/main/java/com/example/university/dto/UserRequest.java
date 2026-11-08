package com.example.university.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class UserRequest extends ProfileRequest {

    @NotBlank(groups = OnCreate.class, message = "Username is required")
    @Pattern(groups = OnCreate.class, regexp = "^[A-Za-z0-9._-]{3,50}$",
            message = "Username must be 3-50 characters: letters, digits, '.', '_' or '-'")
    private String username;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
