package com.chatapp.repository;

import com.chatapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    
    @Modifying
    @Query("UPDATE User u SET u.isOnline = :status WHERE u.id = :userId")
    void updateUserOnlineStatus(Long userId, Boolean status);
    
    @Query("SELECT u FROM User u WHERE u.username LIKE %:query% OR u.email LIKE %:query%")
    List<User> searchUsers(String query);
    
    @Query("SELECT u FROM User u WHERE u.username != :currentUsername")
    List<User> findAllExceptCurrent(String currentUsername);
}