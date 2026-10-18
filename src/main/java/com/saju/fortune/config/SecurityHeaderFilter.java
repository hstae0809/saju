package com.saju.fortune.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "SAMEORIGIN");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        response.setHeader(
            "Content-Security-Policy",
            "default-src 'self' https: data: blob:; " +
            "img-src 'self' data: blob: https:; " +
            "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://gcore.jsdelivr.net https:; " +
            "script-src 'self' 'unsafe-inline'; " +
            "font-src 'self' data: https://cdn.jsdelivr.net https://gcore.jsdelivr.net https:; " +
            "connect-src 'self' http://localhost:8080 http://127.0.0.1:8080 https:"
        );
        filterChain.doFilter(request, response);
    }
}
