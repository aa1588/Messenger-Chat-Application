package com.chatapp.controller;

import com.chatapp.model.User;
import com.chatapp.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String query, Authentication authentication) {
        List<User> users = userService.searchUsers(query, authentication.getName());
        return ResponseEntity.ok(users);
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(Authentication authentication) {
        List<User> users = userService.getAllUsersExceptCurrent(authentication.getName());
        return ResponseEntity.ok(users);
    }

    @PostMapping("/status")
    public ResponseEntity<?> updateOnlineStatus(@RequestParam Boolean isOnline, Authentication authentication) {
        userService.updateOnlineStatus(authentication.getName(), isOnline);
        return ResponseEntity.ok("Status updated");
    }
}