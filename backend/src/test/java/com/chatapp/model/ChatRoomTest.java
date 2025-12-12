package com.chatapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class ChatRoomTest {

    private ChatRoom chatRoom;
    private User creator;
    private User member;

    @BeforeEach
    void setUp() {
        chatRoom = new ChatRoom();
        
        creator = new User();
        creator.setId(1L);
        creator.setUsername("creator");
        
        member = new User();
        member.setId(2L);
        member.setUsername("member");
    }

    @Test
    void setAndGetId_ShouldWorkCorrectly() {
        // Given
        Long id = 1L;

        // When
        chatRoom.setId(id);

        // Then
        assertThat(chatRoom.getId()).isEqualTo(id);
    }

    @Test
    void setAndGetName_ShouldWorkCorrectly() {
        // Given
        String name = "Test Chat Room";

        // When
        chatRoom.setName(name);

        // Then
        assertThat(chatRoom.getName()).isEqualTo(name);
    }

    @Test
    void setAndGetType_ShouldWorkCorrectly() {
        // Given
        ChatRoom.ChatRoomType type = ChatRoom.ChatRoomType.GROUP;

        // When
        chatRoom.setType(type);

        // Then
        assertThat(chatRoom.getType()).isEqualTo(ChatRoom.ChatRoomType.GROUP);
    }

    @Test
    void setAndGetCreatedBy_ShouldWorkCorrectly() {
        // When
        chatRoom.setCreatedBy(creator);

        // Then
        assertThat(chatRoom.getCreatedBy()).isEqualTo(creator);
        assertThat(chatRoom.getCreatedBy().getUsername()).isEqualTo("creator");
    }

    @Test
    void setAndGetCreatedAt_ShouldWorkCorrectly() {
        // Given
        LocalDateTime createdAt = LocalDateTime.now();

        // When
        chatRoom.setCreatedAt(createdAt);

        // Then
        assertThat(chatRoom.getCreatedAt()).isEqualTo(createdAt);
    }

    @Test
    void setAndGetMembers_ShouldWorkCorrectly() {
        // Given
        Set<User> members = new HashSet<>();
        members.add(creator);
        members.add(member);

        // When
        chatRoom.setMembers(members);

        // Then
        assertThat(chatRoom.getMembers()).hasSize(2);
        assertThat(chatRoom.getMembers()).contains(creator, member);
    }

    @Test
    void addMember_ShouldWorkCorrectly() {
        // Given
        chatRoom.setMembers(new HashSet<>());

        // When
        chatRoom.getMembers().add(creator);
        chatRoom.getMembers().add(member);

        // Then
        assertThat(chatRoom.getMembers()).hasSize(2);
        assertThat(chatRoom.getMembers()).contains(creator, member);
    }

    @Test
    void chatRoomTypeEnum_ShouldHaveCorrectValues() {
        // Then
        assertThat(ChatRoom.ChatRoomType.DIRECT).isNotNull();
        assertThat(ChatRoom.ChatRoomType.GROUP).isNotNull();
    }

    @Test
    void constructor_WithParameters_ShouldSetFields() {
        // Given
        String name = "Test Room";
        ChatRoom.ChatRoomType type = ChatRoom.ChatRoomType.DIRECT;

        // When
        ChatRoom room = new ChatRoom(name, type, creator);

        // Then
        assertThat(room.getName()).isEqualTo(name);
        assertThat(room.getType()).isEqualTo(type);
        assertThat(room.getCreatedBy()).isEqualTo(creator);
    }

    @Test
    void prePersist_ShouldSetCreatedAt() {
        // Given
        LocalDateTime beforePersist = LocalDateTime.now();

        // When
        chatRoom.onCreate(); // Simulate @PrePersist

        // Then
        assertThat(chatRoom.getCreatedAt()).isNotNull();
        assertThat(chatRoom.getCreatedAt()).isAfterOrEqualTo(beforePersist);
    }

    @Test
    void completeChatRoom_ShouldHaveAllFields() {
        // Given
        Long id = 1L;
        String name = "Complete Chat Room";
        ChatRoom.ChatRoomType type = ChatRoom.ChatRoomType.GROUP;
        LocalDateTime createdAt = LocalDateTime.now();
        Set<User> members = new HashSet<>();
        members.add(creator);
        members.add(member);

        // When
        chatRoom.setId(id);
        chatRoom.setName(name);
        chatRoom.setType(type);
        chatRoom.setCreatedBy(creator);
        chatRoom.setCreatedAt(createdAt);
        chatRoom.setMembers(members);

        // Then
        assertThat(chatRoom.getId()).isEqualTo(id);
        assertThat(chatRoom.getName()).isEqualTo(name);
        assertThat(chatRoom.getType()).isEqualTo(type);
        assertThat(chatRoom.getCreatedBy()).isEqualTo(creator);
        assertThat(chatRoom.getCreatedAt()).isEqualTo(createdAt);
        assertThat(chatRoom.getMembers()).hasSize(2);
        assertThat(chatRoom.getMembers()).contains(creator, member);
    }

    @Test
    void defaultConstructor_ShouldInitializeEmptyMembers() {
        // When
        ChatRoom room = new ChatRoom();

        // Then
        assertThat(room.getMembers()).isNotNull();
        assertThat(room.getMembers()).isEmpty();
    }
}