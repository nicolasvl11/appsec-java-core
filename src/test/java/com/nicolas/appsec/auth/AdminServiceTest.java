package com.nicolas.appsec.auth;

import com.nicolas.appsec.api.PageResponse;
import com.nicolas.appsec.audit.AuditEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock UserRepository userRepository;
    @Mock AuditEventService auditService;
    @InjectMocks AdminService adminService;

    private User bob;

    @BeforeEach
    void setUp() {
        bob = new User("bob", "hashed", Role.USER);
    }

    // ── listUsers ────────────────────────────────────────────────────────────

    @Test
    void listUsers_returns_page_of_summaries() {
        var pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        var page = new PageImpl<>(List.of(bob), pageRequest, 1);
        when(userRepository.findAll(pageRequest)).thenReturn(page);

        PageResponse<UserSummary> result = adminService.listUsers(0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).username()).isEqualTo("bob");
        assertThat(result.content().get(0).role()).isEqualTo("USER");
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @Test
    void listUsers_empty_page_returns_empty_content() {
        var pageRequest = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(userRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of()));

        PageResponse<UserSummary> result = adminService.listUsers(0, 20);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
    }

    // ── updateRole ───────────────────────────────────────────────────────────

    @Test
    void updateRole_promotes_user_and_records_audit() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

        UserSummary result = adminService.updateRole(2L, Role.ADMIN, "admin");

        assertThat(result.username()).isEqualTo("bob");
        assertThat(result.role()).isEqualTo("ADMIN");
        verify(auditService).recordSecurityEvent(
                eq("admin"),
                eq("role_change"),
                eq("/api/v1/admin/users/2"),
                argThat(m -> "ADMIN".equals(((Map<?, ?>) m).get("newRole")))
        );
    }

    @Test
    void updateRole_throws_conflict_when_admin_changes_own_role() {
        var admin = new User("admin", "hashed", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.updateRole(1L, Role.USER, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot change your own role");

        verifyNoInteractions(auditService);
    }

    @Test
    void updateRole_throws_not_found_for_unknown_id() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateRole(99L, Role.ADMIN, "admin"))
                .isInstanceOf(UsernameNotFoundException.class);

        verifyNoInteractions(auditService);
    }

    @Test
    void updateRole_demotes_admin_to_user() {
        var adminUser = new User("bob", "hashed", Role.ADMIN);
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        UserSummary result = adminService.updateRole(2L, Role.USER, "admin");

        assertThat(result.role()).isEqualTo("USER");
    }

    // ── updateEnabled ────────────────────────────────────────────────────────

    @Test
    void updateEnabled_disables_user_and_records_audit() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

        UserSummary result = adminService.updateEnabled(2L, false, "admin");

        assertThat(result.enabled()).isFalse();
        verify(auditService).recordSecurityEvent(eq("admin"), eq("user_disabled"),
                eq("/api/v1/admin/users/2"), any());
    }

    @Test
    void updateEnabled_enables_user_and_records_audit() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

        UserSummary result = adminService.updateEnabled(2L, true, "admin");

        assertThat(result.enabled()).isTrue();
        verify(auditService).recordSecurityEvent(eq("admin"), eq("user_enabled"),
                eq("/api/v1/admin/users/2"), any());
    }

    @Test
    void updateEnabled_throws_conflict_when_admin_disables_self() {
        var admin = new User("admin", "hashed", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.updateEnabled(1L, false, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot change your own account status");

        verifyNoInteractions(auditService);
    }

    @Test
    void updateEnabled_throws_not_found_for_unknown_id() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.updateEnabled(99L, false, "admin"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    // ── deleteUser ───────────────────────────────────────────────────────────

    @Test
    void deleteUser_removes_user_and_records_audit() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(bob));

        adminService.deleteUser(2L, "admin");

        verify(userRepository).delete(bob);
        verify(auditService).recordSecurityEvent(eq("admin"), eq("user_deleted"),
                eq("/api/v1/admin/users/2"), any());
    }

    @Test
    void deleteUser_throws_conflict_when_admin_deletes_self() {
        var admin = new User("admin", "hashed", Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> adminService.deleteUser(1L, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Cannot delete your own account");

        verify(userRepository, never()).delete(any());
        verifyNoInteractions(auditService);
    }

    @Test
    void deleteUser_throws_not_found_for_unknown_id() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.deleteUser(99L, "admin"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
