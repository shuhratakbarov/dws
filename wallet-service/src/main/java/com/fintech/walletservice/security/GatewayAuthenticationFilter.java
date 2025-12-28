package com.fintech.walletservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter that extracts user information from headers set by API Gateway.
 * The Gateway validates JWT and forwards user info in X-User-* headers.
 */
@Component
@Slf4j
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String userId = request.getHeader(USER_ID_HEADER);
        String userEmail = request.getHeader(USER_EMAIL_HEADER);
        String userRoles = request.getHeader(USER_ROLES_HEADER);

        if (userId != null && userEmail != null) {
            // Store in request attributes for use in controllers/services
            request.setAttribute("userId", userId);
            request.setAttribute("userEmail", userEmail);
            request.setAttribute("userRoles", userRoles);

            // Set in security context holder
            UserContext.setCurrentUser(userId, userEmail, userRoles);

            log.debug("Request authenticated via Gateway: user={}, email={}", userId, userEmail);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clean up thread-local after request
            UserContext.clear();
        }
    }
}

