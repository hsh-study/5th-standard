package com.example.demo.chat.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@Table(
        name = "chat_participants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_chat_participant_room_member",
                columnNames = {"room_id", "member_id"}
        ),
        indexes = @Index(name = "idx_chat_participant_member", columnList = "member_id,room_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    private ChatParticipant(String roomId, String memberId, Instant joinedAt) {
        this.roomId = roomId;
        this.memberId = memberId;
        this.joinedAt = joinedAt;
    }

    public static ChatParticipant create(String liveSaleId, String memberId, Instant now) {
        return new ChatParticipant(liveSaleId, memberId, now);
    }

}
