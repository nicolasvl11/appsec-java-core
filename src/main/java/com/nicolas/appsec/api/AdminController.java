package com.nicolas.appsec.api;

import com.nicolas.appsec.auth.AdminService;
import com.nicolas.appsec.auth.UpdateRoleRequest;
import com.nicolas.appsec.auth.UserSummary;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Validated
@Tag(name = "Admin", description = "Endpoints restricted to the ADMIN role")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/api/v1/admin")
    @Operation(summary = "Admin status check", description = "Returns 200 for authenticated ADMIN users; 403 otherwise.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Caller has ADMIN role"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Authenticated but not ADMIN")
    })
    public Map<String, Object> admin() {
        return Map.of("status", "ok", "area", "admin");
    }

    @GetMapping("/api/v1/admin/users")
    @Operation(summary = "List all users (paginated)", description = "Returns all registered users sorted by creation date.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of users"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Not ADMIN")
    })
    public PageResponse<UserSummary> listUsers(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return adminService.listUsers(page, size);
    }

    @PatchMapping("/api/v1/admin/users/{id}/role")
    @Operation(summary = "Change a user's role", description = "Cannot change your own role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT"),
            @ApiResponse(responseCode = "403", description = "Not ADMIN"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "409", description = "Cannot change own role")
    })
    public UserSummary updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request,
            @AuthenticationPrincipal UserDetails principal
    ) {
        return adminService.updateRole(id, request.role(), principal.getUsername());
    }
}
