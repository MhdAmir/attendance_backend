package com.backend.filter;

import com.backend.service.PasetoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PasetoAuthenticationFilter extends OncePerRequestFilter {
    
    private final PasetoService pasetoService;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    public PasetoAuthenticationFilter(PasetoService pasetoService) {
        this.pasetoService = pasetoService;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Skip authentication for public endpoints
        String path = request.getRequestURI();
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            // Extract token from Authorization header
            String token = extractTokenFromRequest(request);
            
            if (token == null || token.isEmpty()) {
                sendUnauthorizedResponse(response, "Missing or invalid Authorization header");
                return;
            }
            
            // Validate token
            if (!pasetoService.isAccessToken(token)) {
                sendUnauthorizedResponse(response, "Invalid access token");
                return;
            }
            
            // Extract user information and set in request attributes
            Long userId = pasetoService.getUserIdFromToken(token);
            String username = pasetoService.getUsernameFromToken(token);
            
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            
            // Continue filter chain
            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            sendUnauthorizedResponse(response, "Authentication failed: " + e.getMessage());
        }
    }
    
    /**
     * Extract JWT token from Authorization header
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }
    
    /**
     * Check if the endpoint is public (doesn't require authentication)
     */
    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/swagger-resources") ||
               path.startsWith("/webjars/") ||
               path.equals("/");
    }
    
    /**
     * Send 401 Unauthorized response
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"success\": false, \"message\": \"%s\", \"data\": null}", 
            message
        ));
    }
}
