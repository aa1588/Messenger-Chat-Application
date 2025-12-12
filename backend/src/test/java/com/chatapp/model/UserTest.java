package com.chatapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;

class UserTest {

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    void setAndGetId_ShouldWorkCorrectly() {
        // Given
        Long id = 1L;

        // When
        user.setId(id);

        // Then
        assertThat(user.getId()).isEqualTo(id);
    }

    @Test
    void setAndGetUsername_ShouldWorkCorrectly() {
        // Given
        String username = "testuser";

        // When
        user.setUsername(username);

        // Then
        assertThat(user.getUsername()).isEqualTo(username);
    }

    @Test
    void setAndGetEmail_ShouldWorkCorrectly() {
        // Given
        String email = "test@example.com";

        // When
        user.setEmail(email);

        // Then
        assertThat(user.getEmail()).isEqualTo(email);
    }

    @Test
    void setAndGetPassword_ShouldWorkCorrectly() {
        // Given
        String password = "password123";

        // When
        user.setPassword(password);

        // Then
        assertThat(user.getPassword()).isEqualTo(password);
    }

    @Test
    void setAndGetIsOnline_ShouldWorkCorrectly() {
        // Given
        Boolean isOnline = true;

        // When
        user.setIsOnline(isOnline);

        // Then
        assertThat(user.getIsOnline()).isTrue();
    }

    @Test
    void setAndGetLastSeen_ShouldWorkCorrectly() {
        // Given
        LocalDateTime lastSeen = LocalDateTime.now();

        // When
        user.setLastSeen(lastSeen);

        // Then
        assertThat(user.getLastSeen()).isEqualTo(lastSeen);
    }

    @Test
    void defaultValues_ShouldBeCorrect() {
        // Then
        assertThat(user.getId()).isNull();
        assertThat(user.getUsername()).isNull();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPassword()).isNull();
        // Note: isOnline defaults to false, not null
        assertThat(user.getIsOnline()).isFalse();
        assertThat(user.getLastSeen()).isNull();
    }

    @Test
    void userCreation_WithAllFields_ShouldWorkCorrectly() {
        // Given
        Long id = 1L;
        String username = "testuser";
        String email = "test@example.com";
        String password = "password123";
        Boolean isOnline = true;
        LocalDateTime lastSeen = LocalDateTime.now();

        // When
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setIsOnline(isOnline);
        user.setLastSeen(lastSeen);

        // Then
        assertThat(user.getId()).isEqualTo(id);
        assertThat(user.getUsername()).isEqualTo(username);
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getPassword()).isEqualTo(password);
        assertThat(user.getIsOnline()).isTrue();
        assertThat(user.getLastSeen()).isEqualTo(lastSeen);
    }
}