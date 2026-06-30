package com.ganesh.EV_Project.config;

import com.ganesh.EV_Project.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import io.jsonwebtoken.Claims;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.ResponseCookie;
import org.springframework.http.HttpHeaders;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;
        boolean fromCookie = false;

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
        } else if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    fromCookie = true;
                    break;
                }
            }
        }

        if (jwt != null) {
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception e) {
                logger.error("JWT Token extraction failed: " + e.getMessage());
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
            if (jwtUtil.validateToken(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Sliding expiration for cookie-based authentication
                if (fromCookie) {
                    try {
                        java.util.Date expirationDate = jwtUtil.extractExpiration(jwt);
                        long remainingTimeMs = expirationDate.getTime() - System.currentTimeMillis();
                        long totalDurationMs = jwtUtil.getExpiration();

                        if (remainingTimeMs < totalDurationMs / 2) {
                            Claims oldClaims = jwtUtil.extractAllClaims(jwt);
                            Map<String, Object> claims = new HashMap<>(oldClaims);
                            claims.remove("sub");
                            claims.remove("iat");
                            claims.remove("exp");

                            String newToken = jwtUtil.generateToken(username, claims);

                            ResponseCookie newCookie = ResponseCookie.from("token", newToken)
                                    .httpOnly(true)
                                    .secure(true)
                                    .path("/")
                                    .maxAge(totalDurationMs / 1000)
                                    .sameSite("Lax")
                                    .build();
                            response.addHeader(HttpHeaders.SET_COOKIE, newCookie.toString());
                        }
                    } catch (Exception e) {
                        logger.error("Sliding expiration failed: " + e.getMessage());
                    }
                }
            }
        }
        chain.doFilter(request, response);
    }
}
