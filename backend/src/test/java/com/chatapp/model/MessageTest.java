package com.chatapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class MessageTest {

    private Message message;
    private User sender;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        message = new Message();
        
        sender = new User();
        sender.setId(1L);
        sender.setUsername("testuser");
        
        chatRoom = new ChatRoom();
        chatRoom.setId(1L);
        chatRoom.setName("Test Room");
    }

    @Test
    void setAndGetId_ShouldWorkCorrectly() {
        // Given
        Long id = 1L;

        // When
        message.setId(id);

        // Then
        assertThat(message.getId()).isEqualTo(id);
    }

    @Test
    void setAndGetContent_ShouldWorkCorrectly() {
        // Given
        String content = "Test message content";

        // When
        message.setContent(content);

        // Then
        assertThat(message.getContent()).isEqualTo(content);
    }

    @Test
    void setAndGetSender_ShouldWorkCorrectly() {
        // When
        message.setSender(sender);

        // Then
        assertThat(message.getSender()).isEqualTo(sender);
        assertThat(message.getSender().getUsername()).isEqualTo("testuser");
    }

    @Test
    void setAndGetChatRoom_ShouldWorkCorrectly() {
        // When
        message.setChatRoom(chatRoom);

        // Then
        assertThat(message.getChatRoom()).isEqualTo(chatRoom);
        assertThat(message.getChatRoom().getName()).isEqualTo("Test Room");
    }

    @Test
    void setAndGetType_ShouldWorkCorrectly() {
        // Given
        Message.MessageType type = Message.MessageType.CHAT;

        // When
        message.setType(type);

        // Then
        assertThat(message.getType()).isEqualTo(Message.MessageType.CHAT);
    }

    @Test
    void setAndGetCreatedAt_ShouldWorkCorrectly() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now();

        // When
        message.setCreatedAt(createdAt);

        // Then
        assertThat(message.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void setAndGetIsRead_ShouldWorkCorrectly() {
        // Given
        Boolean isRead = true;

        // When
        message.setIsRead(isRead);

        // Then
        assertThat(message.getIsRead()).isTrue();
    }

    @Test
    void setAndGetReadAt_ShouldWorkCorrectly() {
        // Given
        LocalDateTime readAt = LocalDateTime.now();

        // When
        message.setReadAt(readAt);

        // Then
        assertThat(message.getReadAt()).isEqualTo(readAt);
    }

    @Test
    void messageTypeEnum_ShouldHaveCorrectValues() {
        // Then
        assertThat(Message.MessageType.CHAT).isNotNull();
        assertThat(Message.MessageType.JOIN).isNotNull();
        assertThat(Message.MessageType.LEAVE).isNotNull();
    }

    @Test
    void prePersist_ShouldSetCreatedAt() {
        // Given
        LocalDateTime beforePersist = LocalDateTime.now();

        // When
        message.onCreate(); // Simulate @PrePersist

        // Then
        assertThat(message.getCreatedAt()).isNotNull();
        assertThat(message.getCreatedAt()).isAfterOrEqualTo(beforePersist);
    }

    @Test
    void completeMessage_ShouldHaveAllFields() {
        // Given
        Long id = 1L;
        String content = "Complete test message";
        Message.MessageType type = Message.MessageType.CHAT;
        LocalDateTime createdAt = LocalDateTime.now();
        Boolean isRead = false;

        // When
        message.setId(id);
        message.setContent(content);
        message.setSender(sender);
        message.setChatRoom(chatRoom);
        message.setType(type);
        message.setCreatedAt(createdAt);
        message.setIsRead(isRead);

        // Then
        assertThat(message.getId()).isEqualTo(id);
        assertThat(message.getContent()).isEqualTo(content);
        assertThat(message.getSender()).isEqualTo(sender);
        assertThat(message.getChatRoom()).isEqualTo(chatRoom);
        assertThat(message.getType()).isEqualTo(type);
        assertThat(message.getCreatedAt()).isEqualTo(createdAt);
        assertThat(message.getIsRead()).isFalse();
    }
}