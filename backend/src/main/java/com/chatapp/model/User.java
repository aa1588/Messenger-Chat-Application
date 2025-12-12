package com.chatapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    @Column(unique = true)
    private String username;

    @NotBlank
    @Size(max = 100)
    @Email
    @Column(unique = true)
    private String email;

    @NotBlank
    @Size(max = 100)
    @JsonIgnore
    private String password;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;

    @Column(name = "is_online")
    private Boolean isOnline = false;

    @ManyToMany(mappedBy = "members")
    @JsonIgnore
    private Set<ChatRoom> chatRooms = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastSeen = LocalDateTime.now();
    }

    // Constructors
    public User() {}

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(LocalDateTime lastSeen) { this.lastSeen = lastSeen; }

    public Boolean getIsOnline() { return isOnline; }
    public void setIsOnline(Boolean isOnline) { this.isOnline = isOnline; }

    public Set<ChatRoom> getChatRooms() { return chatRooms; }
    public void setChatRooms(Set<ChatRoom> chatRooms) { this.chatRooms = chatRooms; }
}