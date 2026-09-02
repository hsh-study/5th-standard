package com.example.demo.chat.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "chat_messages",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_chat_message_room_client",
        columnNames = {"room_id", "client_message_id"}
    ),
    indexes = @Index(name = "idx_chat_message_room_sequence", columnList = "room_id,id")
)
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "message_id", nullable = false, unique = true, updatable = false)
    private UUID messageId;

    @Column(name = "room_id", nullable = false, updatable = false, length = 100)
    private String roomId;

    @Column(name = "sender_id", nullable = false, updatable = false, length = 100)
    private String senderId;

    @Column(name = "client_message_id", nullable = false, updatable = false, length = 100)
    private String clientMessageId;

    @Column(nullable = false, updatable = false, length = 1000)
    private String content;

    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected ChatMessage() {
    }

    public ChatMessage(
        UUID messageId,
        String roomId,
        String senderId,
        String clientMessageId,
        String content,
        Instant sentAt
    ) {
        this.messageId = messageId;
        this.roomId = roomId;
        this.senderId = senderId;
        this.clientMessageId = clientMessageId;
        this.content = content;
        this.sentAt = sentAt;
    }

    public ChatMessage(
        UUID messageId,
        String roomId,
        long sequence,
        String senderId,
        String clientMessageId,
        String content,
        Instant sentAt
    ) {
        this(messageId, roomId, senderId, clientMessageId, content, sentAt);
        this.id = sequence;
    }

    public Long getId() {
        return id;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public String getRoomId() {
        return roomId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String clientMessageId() {
        return clientMessageId;
    }

    public String getContent() {
        return content;
    }

    public Instant getSentAt() {
        return sentAt;
    }

}
