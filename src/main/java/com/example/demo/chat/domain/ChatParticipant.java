package com.example.demo.chat.domain;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "chat_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_participant_room_member",
                columnNames = {"room_id", "member_id"}
        ),
        indexes = @Index(name = "idx_chat_participant_member", columnList = "member_id,room_id")
)
public class ChatParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, updatable = false, length = 100)
    private String roomId;

    @Column(name = "member_id", nullable = false, updatable = false, length = 100)
    private String memberId;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected ChatParticipant() {
    }

    public ChatParticipant(String roomId, String memberId, Instant joinedAt) {
        this.roomId = roomId;
        this.memberId = memberId;
        this.joinedAt = joinedAt;
    }

    public String getRoom() { return roomId; }
    public String getMember() { return memberId; }
    public Instant getJoinedAt() { return joinedAt; }
}
