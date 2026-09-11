package com.example.demo.chat.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "chat_read_states",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_chat_read_state_room_member",
        columnNames = {"room_id", "member_id"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatReadState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, updatable = false, length = 100)
    private String roomId;

    @Column(name = "member_id", nullable = false, updatable = false, length = 100)
    private String memberId;

    @Column(name = "last_read_id", nullable = false)
    private long lastReadId;

    private ChatReadState(String roomId, String memberId) {
        this.roomId = roomId;
        this.memberId = memberId;
    }

    public static ChatReadState create(String roomId, String memberId) {
        return new ChatReadState(roomId, memberId);
    }

    public void updateLastReadId(long lastReadId) {
        // this.lastReadId = 3, lastReadId = 2
        if (this.lastReadId < lastReadId) {
            this.lastReadId = lastReadId;
        }
    }
}
