package com.chatapp.repository;

import com.chatapp.model.Message;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatRoomOrderByCreatedAtAsc(ChatRoom chatRoom);
    Page<Message> findByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom, Pageable pageable);
    Optional<Message> findTopByChatRoomOrderByCreatedAtDesc(ChatRoom chatRoom);
    
    @Query("SELECT COUNT(m) FROM Message m WHERE m.chatRoom = :chatRoom AND m.sender != :user AND (m.isRead = false OR m.isRead IS NULL)")
    int countUnreadMessagesForUser(@Param("chatRoom") ChatRoom chatRoom, @Param("user") User user);
}