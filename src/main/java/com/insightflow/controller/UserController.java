package com.insightflow.controller;

import com.insightflow.dto.ApiResponse;
import com.insightflow.dto.UpdateUserRequest;
import com.insightflow.dto.UserResponse;
import com.insightflow.entity.User;
import com.insightflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = "Users", description = "User management endpoints")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        UserResponse response = userService.getUserById(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal User currentUser) {
        UserResponse response = userService.updateUser(id, request, currentUser);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Integer id,
            @AuthenticationPrincipal User currentUser) {
        userService.deleteUser(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("User account deleted successfully"));
    }


    @PutMapping("/{id}/restore")
public UserResponse restoreUser(@PathVariable Integer id) {
    return userService.restoreUser(id);
}

}
