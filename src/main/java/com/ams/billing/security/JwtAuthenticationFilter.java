package com.ams.billing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Processes every incoming request's Authorization: Bearer <GATEWAY_JWT> header.
 *
 * The Gateway JWT can be one of two types:
 *
 *   type: "user"    → came from a frontend user login. Has roles claim.
 *                     Sets authentication with ROLE_FINANCE_OFFICER etc.
 *
 *   type: "service" → came from another backend service via the Gateway.
 *                     Has sub = calling service name. No roles claim.
 *                     Sets authentication with ROLE_SERVICE authority.
 *
 * @PreAuthorize on controllers then enforces what each type can do.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractToken(request);
        log.info("Incoming request: {} {} | Token present: {}", request.getMethod(), request.getRequestURI(), StringUtils.hasText(token));

        if (StringUtils.hasText(token)) {
            if (jwtTokenProvider.validateToken(token)) {
                try {
                    String tokenType = jwtTokenProvider.extractTokenType(token);
                    String subject   = jwtTokenProvider.extractSubject(token);

                    List<SimpleGrantedAuthority> authorities;

                    if ("service".equals(tokenType)) {
                        authorities = List.of(new SimpleGrantedAuthority("SERVICE"));
                        log.info("Service JWT authenticated: callingService={}", subject);
                    } else {
                        List<String> roles = jwtTokenProvider.extractRoles(token);
                        authorities = roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList();
                        log.info("User JWT authenticated: userId={}, roles={}", subject, roles);
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(subject, null, authorities);

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("SecurityContext successfully set for subject: {}", subject);

                } catch (Exception ex) {
                    log.error("Failed to process Gateway JWT: {}", ex.getMessage());
                    SecurityContextHolder.clearContext();
                }
            } else {
                log.warn("JWT Token validation failed for token: {}", token.substring(0, Math.min(token.length(), 15)) + "...");
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}