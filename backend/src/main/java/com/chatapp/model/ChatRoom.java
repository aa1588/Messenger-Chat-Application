package com.chatapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chat_rooms")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChatRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    private ChatRoomType type;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToMany
    @JoinTable(
        name = "chat_room_members",
        joinColumns = @JoinColumn(name = "chat_room_id"),
        inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @JsonIgnoreProperties("chatRooms")
    private Set<User> members = new HashSet<>();

    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("chatRoom")
    private Set<Message> messages = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Constructors
    public ChatRoom() {}

    public ChatRoom(String name, ChatRoomType type, User createdBy) {
        this.name = name;
        this.type = type;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ChatRoomType getType() { return type; }
    public void setType(ChatRoomType type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Set<User> getMembers() { return members; }
    public void setMembers(Set<User> members) { this.members = members; }

    public Set<Message> getMessages() { return messages; }
    public void setMessages(Set<Message> messages) { this.messages = messages; }

    public enum ChatRoomType {
        DIRECT, GROUP
    }
}