package com.nicolas.appsec.auth;

import com.nicolas.appsec.api.PageResponse;
import com.nicolas.appsec.audit.AuditEventService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AuditEventService auditService;

    public AdminService(UserRepository userRepository, AuditEventService auditService) {
        this.userRepository = userRepository;
        this.auditService   = auditService;
    }

    public PageResponse<UserSummary> listUsers(int page, int size) {
        return PageResponse.from(
                userRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                        .map(UserSummary::from)
        );
    }

    @Transactional
    public UserSummary updateRole(Long id, Role newRole, String adminUsername) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + id));

        if (user.getUsername().equals(adminUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change your own role.");
        }

        user.updateRole(newRole);
        auditService.recordSecurityEvent(adminUsername, "role_change", "/api/v1/admin/users/" + id,
                Map.of("userId", id, "newRole", newRole.name(), "username", user.getUsername()));

        return UserSummary.from(user);
    }
}
