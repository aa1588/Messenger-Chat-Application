package com.chatapp.service;

import com.chatapp.model.User;
import com.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<User> searchUsers(String query, String currentUsername) {
        return userRepository.searchUsers(query)
                .stream()
                .filter(user -> !user.getUsername().equals(currentUsername))
                .collect(Collectors.toList());
    }

    public List<User> getAllUsersExceptCurrent(String currentUsername) {
        return userRepository.findAllExceptCurrent(currentUsername);
    }

    @Transactional
    public void updateOnlineStatus(String username, Boolean isOnline) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setIsOnline(isOnline);
        user.setLastSeen(LocalDateTime.now());
        User savedUser = userRepository.save(user);
        
        // Broadcast online status change to all users
        broadcastOnlineStatusChange(savedUser);
    }

    private void broadcastOnlineStatusChange(User user) {
        // Create a simple status update object
        var statusUpdate = new Object() {
            public final Long userId = user.getId();
            public final String username = user.getUsername();
            public final Boolean isOnline = user.getIsOnline();
            public final String lastSeen = user.getLastSeen().toString();
        };
        
        // Broadcast to all connected users
        messagingTemplate.convertAndSend("/topic/user-status", statusUpdate);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}