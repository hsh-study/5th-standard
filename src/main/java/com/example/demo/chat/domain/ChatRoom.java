package com.example.demo.chat.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    @Column(name = "room_id", length = 100)
    private String id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ChatRoom() {
    }

    public ChatRoom(String id) {
        this(id, Instant.now());
    }

    public ChatRoom(String id, Instant createdAt) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("chat room id must not be blank");
        }
        this.id = id;
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public String getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }
}

