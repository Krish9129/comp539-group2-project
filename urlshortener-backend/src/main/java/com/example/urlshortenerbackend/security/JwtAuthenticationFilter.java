package com.example.urlshortenerbackend.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");
        System.out.println("JWT Filter - Auth Header: " + authorizationHeader); // 添加调试日志

        String username = null;
        String jwt = null;
        Map<String, Object> claims = null;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            System.out.println("JWT Filter - Token extracted: " + jwt.substring(0, Math.min(jwt.length(), 20)) + "..."); // 添加调试日志

            try {
                username = jwtUtil.extractUsername(jwt);
                claims = jwtUtil.extractAllClaimsAsMap(jwt);
                System.out.println("JWT Filter - Username: " + username); // 添加调试日志
            } catch (Exception e) {
                System.out.println("JWT Filter - Token validation error: " + e.getMessage()); // 添加调试日志
            }
        } else {
            System.out.println("JWT Filter - No token found or incorrect format"); // 添加调试日志
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(jwt)) { // 现在只使用一个参数
                Map<String, Object> attributes = new HashMap<>(claims);

                DefaultOAuth2User oauth2User = new DefaultOAuth2User(
                        Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                        attributes,
                        "email"
                );

                OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                        oauth2User,
                        oauth2User.getAuthorities(),
                        (String) claims.get("provider")
                );

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(request, response);
    }
}