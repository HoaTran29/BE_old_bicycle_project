package com.backend.old_bicycle_project.security;

import java.security.Principal;

public class StompUserPrincipal implements Principal {

    private final String userId;
    private final String email;

    public StompUserPrincipal(String userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    @Override
    public String getName() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}
