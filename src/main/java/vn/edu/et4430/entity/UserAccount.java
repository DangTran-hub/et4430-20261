package vn.edu.et4430.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_account")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "username", nullable = false, columnDefinition = "text", unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    @Column(name = "full_name", nullable = false, columnDefinition = "text")
    private String fullName;

    @Column(name = "email", nullable = true, columnDefinition = "text")
    private String email;

    @Column(name = "role", nullable = false, columnDefinition = "text")
    private String role;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public UserAccount() {
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

