package com.schoolms.user;

import com.schoolms.common.api.ApiResponse;
import com.schoolms.user.dto.CreateUserRequest;
import com.schoolms.user.dto.LocaleRequest;
import com.schoolms.user.dto.RoleRequest;
import com.schoolms.user.dto.UpdateUserRequest;
import com.schoolms.user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    @Operation(summary = "List users in the current school")
    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping
    public ApiResponse<List<UserDto>> list() {
        return ApiResponse.ok(userService.list());
    }

    @Operation(summary = "Get a user")
    @PreAuthorize("hasAuthority('USER_READ')")
    @GetMapping("/{id}")
    public ApiResponse<UserDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(userService.get(id));
    }

    @Operation(summary = "Create a user and assign roles")
    @PreAuthorize("hasAuthority('USER_CREATE')")
    @PostMapping
    public ApiResponse<UserDto> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @Operation(summary = "Update a user")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    @PatchMapping("/{id}")
    public ApiResponse<UserDto> update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @Operation(summary = "Assign a role to a user")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    @PostMapping("/{id}/roles")
    public ApiResponse<UserDto> assignRole(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return ApiResponse.ok(userService.assignRole(id, request));
    }

    @Operation(summary = "Remove a role from a user")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    @DeleteMapping("/{id}/roles")
    public ApiResponse<UserDto> removeRole(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return ApiResponse.ok(userService.removeRole(id, request));
    }

    @Operation(summary = "Update the current user's language preference")
    @PutMapping("/me/locale")
    public ApiResponse<Void> updateMyLocale(@Valid @RequestBody LocaleRequest request) {
        userService.updateMyLocale(request);
        return ApiResponse.okMessage("user.locale_updated");
    }
}
