package com.chatapp.repository;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
class MessageRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MessageRepository messageRepository;

    private User testUser;
    private User otherUser;
    private ChatRoom testChatRoom;
    private Message message1;
    private Message message2;
    private Message message3;

    @BeforeEach
    void setUp() {
        // Create users
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("password");
        testUser = entityManager.persistAndFlush(testUser);

        otherUser = new User();
        otherUser.setUsername("otheruser");
        otherUser.setEmail("other@example.com");
        otherUser.setPassword("password");
        otherUser = entityManager.persistAndFlush(otherUser);

        // Create chat room
        testChatRoom = new ChatRoom();
        testChatRoom.setName("Test Room");
        testChatRoom.setType(ChatRoom.ChatRoomType.GROUP);
        testChatRoom.setCreatedBy(testUser);
        testChatRoom = entityManager.persistAndFlush(testChatRoom);

        // Create messages
        message1 = new Message();
        message1.setContent("First message");
        message1.setSender(testUser);
        message1.setChatRoom(testChatRoom);
        message1.setType(Message.MessageType.CHAT);
        message1.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        message1.setIsRead(true);
        message1 = entityManager.persistAndFlush(message1);

        message2 = new Message();
        message2.setContent("Second message");
        message2.setSender(otherUser);
        message2.setChatRoom(testChatRoom);
        message2.setType(Message.MessageType.CHAT);
        message2.setCreatedAt(LocalDateTime.now().minusMinutes(5));
        message2.setIsRead(false);
        message2 = entityManager.persistAndFlush(message2);

        message3 = new Message();
        message3.setContent("Third message");
        message3.setSender(otherUser);
        message3.setChatRoom(testChatRoom);
        message3.setType(Message.MessageType.CHAT);
        message3.setCreatedAt(LocalDateTime.now());
        message3.setIsRead(false);
        message3 = entityManager.persistAndFlush(message3);

        entityManager.clear();
    }

    @Test
    void findByChatRoomOrderByCreatedAtAsc_ShouldReturnMessagesInAscendingOrder() {
        // When
        List<Message> messages = messageRepository.findByChatRoomOrderByCreatedAtAsc(testChatRoom);

        // Then
        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getContent()).isEqualTo("First message");
        assertThat(messages.get(1).getContent()).isEqualTo("Second message");
        assertThat(messages.get(2).getContent()).isEqualTo("Third message");
    }

    @Test
    void findTopByChatRoomOrderByCreatedAtDesc_ShouldReturnLatestMessage() {
        // When
        Optional<Message> latestMessage = messageRepository.findTopByChatRoomOrderByCreatedAtDesc(testChatRoom);

        // Then
        assertThat(latestMessage).isPresent();
        assertThat(latestMessage.get().getContent()).isEqualTo("Third message");
    }

    @Test
    void countUnreadMessagesForUser_ShouldReturnCorrectCount() {
        // When
        int unreadCount = messageRepository.countUnreadMessagesForUser(testChatRoom, testUser);

        // Then
        assertThat(unreadCount).isEqualTo(2); // message2 and message3 are unread and not from testUser
    }

    @Test
    void countUnreadMessagesForUser_WithNoUnreadMessages_ShouldReturnZero() {
        // Given - mark all messages as read
        message2.setIsRead(true);
        message3.setIsRead(true);
        entityManager.merge(message2);
        entityManager.merge(message3);
        entityManager.flush();
        entityManager.clear();

        // When
        int unreadCount = messageRepository.countUnreadMessagesForUser(testChatRoom, testUser);

        // Then
        assertThat(unreadCount).isEqualTo(0);
    }

    @Test
    void countUnreadMessagesForUser_ShouldExcludeOwnMessages() {
        // Given - create unread message from testUser
        Message ownMessage = new Message();
        ownMessage.setContent("Own message");
        ownMessage.setSender(testUser);
        ownMessage.setChatRoom(testChatRoom);
        ownMessage.setType(Message.MessageType.CHAT);
        ownMessage.setCreatedAt(LocalDateTime.now().plusMinutes(1));
        ownMessage.setIsRead(false);
        entityManager.persistAndFlush(ownMessage);
        entityManager.clear();

        // When
        int unreadCount = messageRepository.countUnreadMessagesForUser(testChatRoom, testUser);

        // Then
        assertThat(unreadCount).isEqualTo(2); // Still only message2 and message3, not the own message
    }

    @Test
    void findByChatRoomOrderByCreatedAtAsc_WithEmptyChatRoom_ShouldReturnEmptyList() {
        // Given
        ChatRoom emptyChatRoom = new ChatRoom();
        emptyChatRoom.setName("Empty Room");
        emptyChatRoom.setType(ChatRoom.ChatRoomType.GROUP);
        emptyChatRoom.setCreatedBy(testUser);
        emptyChatRoom = entityManager.persistAndFlush(emptyChatRoom);

        // When
        List<Message> messages = messageRepository.findByChatRoomOrderByCreatedAtAsc(emptyChatRoom);

        // Then
        assertThat(messages).isEmpty();
    }

    @Test
    void findTopByChatRoomOrderByCreatedAtDesc_WithEmptyChatRoom_ShouldReturnEmpty() {
        // Given
        ChatRoom emptyChatRoom = new ChatRoom();
        emptyChatRoom.setName("Empty Room");
        emptyChatRoom.setType(ChatRoom.ChatRoomType.GROUP);
        emptyChatRoom.setCreatedBy(testUser);
        emptyChatRoom = entityManager.persistAndFlush(emptyChatRoom);

        // When
        Optional<Message> latestMessage = messageRepository.findTopByChatRoomOrderByCreatedAtDesc(emptyChatRoom);

        // Then
        assertThat(latestMessage).isEmpty();
    }

    @Test
    void countUnreadMessagesForUser_WithDifferentChatRoom_ShouldReturnZero() {
        // Given
        ChatRoom otherChatRoom = new ChatRoom();
        otherChatRoom.setName("Other Room");
        otherChatRoom.setType(ChatRoom.ChatRoomType.GROUP);
        otherChatRoom.setCreatedBy(testUser);
        otherChatRoom = entityManager.persistAndFlush(otherChatRoom);

        // When
        int unreadCount = messageRepository.countUnreadMessagesForUser(otherChatRoom, testUser);

        // Then
        assertThat(unreadCount).isEqualTo(0);
    }
}