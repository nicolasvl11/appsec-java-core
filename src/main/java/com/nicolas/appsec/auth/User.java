package com.nicolas.appsec.auth;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = true)
    private String password;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = true, length = 255)
    private String email;

    @Column(nullable = true, length = 50)
    private String provider;

    @Column(name = "provider_user_id", nullable = true, length = 255)
    private String providerUserId;

    protected User() {}

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public User(String username, String email, String provider, String providerUserId, Role role) {
        this.username = username;
        this.email = email;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.role = role;
        this.createdAt = Instant.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String getPassword() { return password; }
    @Override public String getUsername() { return username; }

    public Role getRole() { return role; }
    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
    public String getEmail() { return email; }
    public String getProvider() { return provider; }
    public String getProviderUserId() { return providerUserId; }
    public boolean isOAuth2User() { return provider != null; }

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateRole(Role newRole) {
        this.role = newRole;
    }
}
