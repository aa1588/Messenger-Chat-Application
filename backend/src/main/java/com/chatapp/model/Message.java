package com.chatapp.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    @JsonIgnoreProperties({"chatRooms", "password"})
    private User sender;

    @ManyToOne
    @JoinColumn(name = "chat_room_id", nullable = false)
    @JsonIgnoreProperties({"members", "messages"})
    private ChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    private MessageType type = MessageType.CHAT;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "is_delivered")
    private Boolean isDelivered = false;

    @Column(name = "is_read")
    private Boolean isRead = false;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        // Mark as delivered immediately for now (can be enhanced later)
        deliveredAt = LocalDateTime.now();
        isDelivered = true;
    }

    // Constructors
    public Message() {}

    public Message(String content, User sender, ChatRoom chatRoom) {
        this.content = content;
        this.sender = sender;
        this.chatRoom = chatRoom;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getSender() { return sender; }
    public void setSender(User sender) { this.sender = sender; }

    public ChatRoom getChatRoom() { return chatRoom; }
    public void setChatRoom(ChatRoom chatRoom) { this.chatRoom = chatRoom; }

    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }

    public LocalDateTime getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }

    public Boolean getIsDelivered() { return isDelivered; }
    public void setIsDelivered(Boolean isDelivered) { this.isDelivered = isDelivered; }

    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }

    public enum MessageType {
        CHAT, JOIN, LEAVE
    }
}