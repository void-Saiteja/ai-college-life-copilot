package com.collegecopilot.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class UserPrincipal implements UserDetails {

    private final String id;
    private final String name;
    private final String email;
    private final String role;
    private final String studentId;
    private final String adminId;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(String id, String name, String email, String role, String studentId, String adminId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.role = role;
        this.studentId = studentId;
        this.adminId = adminId;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getAdminId() {
        return adminId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null; // Statelessly authenticated via JWT
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
