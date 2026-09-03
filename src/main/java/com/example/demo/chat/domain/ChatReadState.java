package com.example.demo.chat.domain;

import jakarta.persistence.*;

@Entity
@Table(
    name = "chat_read_states",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_chat_read_state_room_member",
        columnNames = {"room_id", "member_id"}
    )
)
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

    protected ChatReadState() {}

    public ChatReadState(String roomId, String memberId) {
        this.roomId = roomId;
        this.memberId = memberId;
    }

    public void updateLastReadId(long lastReadId) {
    }

    public String getRoomId() {
        return roomId;
    }

    public String getMemberId() {
        return memberId;
    }

    public long getLastReadId() {
        return lastReadId;
    }

}
