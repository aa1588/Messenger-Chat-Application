package com.chatapp.repository;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
    List<ChatRoom> findByMembersContaining(User user);
    
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.type = 'DIRECT' AND :user1 MEMBER OF cr.members AND :user2 MEMBER OF cr.members")
    Optional<ChatRoom> findDirectChatRoom(User user1, User user2);
    
    @Query("SELECT cr FROM ChatRoom cr WHERE cr.type = 'GROUP' AND :user MEMBER OF cr.members")
    List<ChatRoom> findGroupChatRooms(User user);
}