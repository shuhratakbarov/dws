package com.fintech.walletservice.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Thread-local context for storing current user information.
 * Set by GatewayAuthenticationFilter, available throughout the request lifecycle.
 */
@Slf4j
public class UserContext {

    private static final ThreadLocal<UserInfo> currentUser = new ThreadLocal<>();

    public static void setCurrentUser(String userId, String email, String roles) {
        List<String> roleList = roles != null
                ? Arrays.asList(roles.split(","))
                : Collections.emptyList();
        currentUser.set(new UserInfo(userId, email, roleList));
    }

    public static UserInfo getCurrentUser() {
        return currentUser.get();
    }

    public static UUID getCurrentUserId() {
        UserInfo user = currentUser.get();
        if (user == null || user.getUserId() == null) {
            return null;
        }
        try {
            return UUID.fromString(user.getUserId());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid user ID format: {}", user.getUserId());
            return null;
        }
    }

    public static String getCurrentUserEmail() {
        UserInfo user = currentUser.get();
        return user != null ? user.getEmail() : null;
    }

    public static boolean isAdmin() {
        UserInfo user = currentUser.get();
        return user != null && user.getRoles().contains("ADMIN");
    }

    public static boolean isAuthenticated() {
        return currentUser.get() != null;
    }

    public static void clear() {
        currentUser.remove();
    }

    @Getter
    public static class UserInfo {
        private final String userId;
        private final String email;
        private final List<String> roles;

        public UserInfo(String userId, String email, List<String> roles) {
            this.userId = userId;
            this.email = email;
            this.roles = roles != null ? roles : Collections.emptyList();
        }
    }
}

