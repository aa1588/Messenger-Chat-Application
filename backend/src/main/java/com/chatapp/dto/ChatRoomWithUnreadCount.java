package com.chatapp.dto;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import java.time.LocalDateTime;
import java.util.Set;

public class ChatRoomWithUnreadCount {
    private Long id;
    private String name;
    private ChatRoom.ChatRoomType type;
    private LocalDateTime createdAt;
    private User createdBy;
    private Set<User> members;
    private Message lastMessage;
    private LocalDateTime lastMessageTime;
    private int unreadCount;

    // Constructors
    public ChatRoomWithUnreadCount() {}

    public ChatRoomWithUnreadCount(ChatRoom chatRoom, int unreadCount) {
        this.id = chatRoom.getId();
        this.name = chatRoom.getName();
        this.type = chatRoom.getType();
        this.createdAt = chatRoom.getCreatedAt();
        this.createdBy = chatRoom.getCreatedBy();
        this.members = chatRoom.getMembers();
        this.unreadCount = unreadCount;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ChatRoom.ChatRoomType getType() { return type; }
    public void setType(ChatRoom.ChatRoomType type) { this.type = type; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Set<User> getMembers() { return members; }
    public void setMembers(Set<User> members) { this.members = members; }

    public Message getLastMessage() { return lastMessage; }
    public void setLastMessage(Message lastMessage) { 
        this.lastMessage = lastMessage;
        if (lastMessage != null) {
            this.lastMessageTime = lastMessage.getCreatedAt();
        }
    }

    public LocalDateTime getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(LocalDateTime lastMessageTime) { this.lastMessageTime = lastMessageTime; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
}