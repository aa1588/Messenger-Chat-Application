package com.chatapp.dto;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.*;

class ChatRoomWithUnreadCountTest {

    private ChatRoom chatRoom;
    private User creator;
    private Message lastMessage;

    @BeforeEach
    void setUp() {
        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");

        chatRoom = new ChatRoom();
        chatRoom.setId(1L);
        chatRoom.setName("Test Room");
        chatRoom.setType(ChatRoom.ChatRoomType.GROUP);
        chatRoom.setCreatedBy(creator);
        chatRoom.setMembers(new HashSet<>());
        chatRoom.setCreatedAt(LocalDateTime.now());

        lastMessage = new Message();
        lastMessage.setId(1L);
        lastMessage.setContent("Last message");
        lastMessage.setSender(creator);
        lastMessage.setChatRoom(chatRoom);
        lastMessage.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void defaultConstructor_ShouldCreateEmptyObject() {
        // When
        ChatRoomWithUnreadCount dto = new ChatRoomWithUnreadCount();

        // Then
        assertThat(dto.getId()).isNull();
        assertThat(dto.getName()).isNull();
        assertThat(dto.getType()).isNull();
        assertThat(dto.getCreatedBy()).isNull();
        assertThat(dto.getMembers()).isNull();
        assertThat(dto.getLastMessage()).isNull();
        assertThat(dto.getLastMessageTime()).isNull();
        assertThat(dto.getUnreadCount()).isEqualTo(0);
    }

    @Test
    void constructorWithChatRoomAndCount_ShouldCopyFields() {
        // Given
        int unreadCount = 5;

        // When
        ChatRoomWithUnreadCount dto = new ChatRoomWithUnreadCount(chatRoom, unreadCount);

        // Then
        assertThat(dto.getId()).isEqualTo(chatRoom.getId());
        assertThat(dto.getName()).isEqualTo(chatRoom.getName());
        assertThat(dto.getType()).isEqualTo(chatRoom.getType());
        assertThat(dto.getCreatedAt()).isEqualTo(chatRoom.getCreatedAt());
        assertThat(dto.getCreatedBy()).isEqualTo(chatRoom.getCreatedBy());
        assertThat(dto.getMembers()).isEqualTo(chatRoom.getMembers());
        assertThat(dto.getUnreadCount()).isEqualTo(unreadCount);
    }

    @Test
    void setLastMessage_ShouldSetMessageAndTime() {
        // Given
        ChatRoomWithUnreadCount dto = new ChatRoomWithUnreadCount();

        // When
        dto.setLastMessage(lastMessage);

        // Then
        assertThat(dto.getLastMessage()).isEqualTo(lastMessage);
        assertThat(dto.getLastMessageTime()).isEqualTo(lastMessage.getCreatedAt());
    }

    @Test
    void setLastMessage_WithNull_ShouldNotSetTime() {
        // Given
        ChatRoomWithUnreadCount dto = new ChatRoomWithUnreadCount();

        // When
        dto.setLastMessage(null);

        // Then
        assertThat(dto.getLastMessage()).isNull();
        assertThat(dto.getLastMessageTime()).isNull();
    }

    @Test
    void setAndGetUnreadCount_ShouldWorkCorrectly() {
        // Given
        ChatRoomWithUnreadCount dto = new ChatRoomWithUnreadCount();
        int unreadCount = 10;

        // When
        dto.setUnreadCount(unreadCount);

        // Then
        assertThat(dto.getUnreadCount()).isEqualTo(unreadCount);
    }

    @Test
    void setAndGetId_ShouldWorkCorrectly() {
        // Given
        ChatRoomWithUnreadCount dto = new ChatRoomWithUnreadCount();
        Long id = 1L;

        // When
        dto.setId(id);

        // Then
        assertThat(dto.getId()).isEqualTo(id);
    }

    @Test
    void setAndGetName_ShouldWorkCorrectly() {
        // Given
        ChatRoomWithUnreadCount dto = new ChatRoomWithUnreadCount();
        String name = "Test Room";

        // When
        dto.setName(name);

        // Then
        assertThat(dto.getName()).isEqualTo(name);
    }

    @Test
    void setAndGetType_ShouldWorkCorrectly() {
        // Given
        ChatRoomWithUnreadCount dto = new ChatRoomWithUnreadCount();
        ChatRoom.ChatRoomType type = ChatRoom.ChatRoomType.DIRECT;

        // When
        dto.setType(type);

        // Then
        assertThat(dto.getType()).isEqualTo(type);
    }

    @Test
    void completeDto_ShouldHaveAllFields() {
        // Given
        int unreadCount = 3;
        ChatRoomWithUnreadCount dto = new ChatRoomWithUnreadCount(chatRoom, unreadCount);

        // When
        dto.setLastMessage(lastMessage);

        // Then
        assertThat(dto.getId()).isEqualTo(chatRoom.getId());
        assertThat(dto.getName()).isEqualTo(chatRoom.getName());
        assertThat(dto.getType()).isEqualTo(chatRoom.getType());
        assertThat(dto.getCreatedAt()).isEqualTo(chatRoom.getCreatedAt());
        assertThat(dto.getCreatedBy()).isEqualTo(chatRoom.getCreatedBy());
        assertThat(dto.getMembers()).isEqualTo(chatRoom.getMembers());
        assertThat(dto.getLastMessage()).isEqualTo(lastMessage);
        assertThat(dto.getLastMessageTime()).isEqualTo(lastMessage.getCreatedAt());
        assertThat(dto.getUnreadCount()).isEqualTo(unreadCount);
    }
}