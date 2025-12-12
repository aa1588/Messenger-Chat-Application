package com.chatapp.controller;

import com.chatapp.dto.MessageRequest;
import com.chatapp.model.Message;
import com.chatapp.service.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    @Autowired
    private ChatService chatService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload MessageRequest messageRequest, SimpMessageHeaderAccessor headerAccessor) {
        String username = (String) headerAccessor.getSessionAttributes().get("username");
        chatService.sendMessage(messageRequest, username);
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload MessageRequest messageRequest, SimpMessageHeaderAccessor headerAccessor) {
        String username = messageRequest.getContent();
        headerAccessor.getSessionAttributes().put("username", username);
        chatService.addUser(messageRequest, username);
    }
}