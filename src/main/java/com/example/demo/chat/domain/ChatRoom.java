package com.example.demo.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@Table(name = "chat_rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom {

    @Id
    @Column(name = "room_id", length = 100)
    private String id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    private ChatRoom(String id, Instant createdAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("chat room id must not be blank");
        }
        this.id = id;
        this.createdAt = Objects.requireNonNull(createdAt);
    }


    public static ChatRoom create(String liveSaleId, Instant now) {
        return new ChatRoom(liveSaleId, now);
    }

}

