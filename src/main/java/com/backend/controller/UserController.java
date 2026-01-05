package com.backend.controller;

import com.backend.dto.ApiResponse;
import com.backend.dto.UserResponse;
import com.backend.entity.User;
import com.backend.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management APIs (Protected)")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {
    
    private final UserRepository userRepository;
    
    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    @GetMapping("/me")
    @Operation(
        summary = "Get current user profile",
        description = "Get the profile of the currently authenticated user"
    )
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UserResponse userResponse = UserResponse.fromUser(user);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", userResponse));
    }
    
    @GetMapping
    @Operation(
        summary = "Get all users",
        description = "Retrieve a list of all registered users (requires authentication)"
    )
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::fromUser)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", userResponses));
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get user by ID",
        description = "Retrieve a specific user by their ID (requires authentication)"
    )
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        UserResponse userResponse = UserResponse.fromUser(user);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", userResponse));
    }
}
