CREATE TABLE IF NOT EXISTS chat_read_states (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id VARCHAR(100) NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    last_read_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_read_state_room_member UNIQUE (room_id, member_id),
    CONSTRAINT fk_chat_read_state_room
        FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id),
    CONSTRAINT fk_chat_read_state_member
        FOREIGN KEY (member_id) REFERENCES members(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO chat_rooms(room_id, created_at) VALUES
    ('sale-1', '2026-08-01 09:00:00');

INSERT IGNORE INTO chat_participants(room_id, member_id, joined_at) VALUES
    ('sale-1', 'member-1', '2026-08-01 09:00:00'),
    ('sale-1', 'member-2', '2026-08-01 09:00:00');

INSERT IGNORE INTO chat_messages(message_id, room_id, sender_id, client_message_id, content, sent_at) VALUES
    (UUID_TO_BIN(UUID()), 'sale-1', 'member-2', 'session-24-001', '강의 기초 메시지 1', '2026-08-01 09:00:01'),
    (UUID_TO_BIN(UUID()), 'sale-1', 'member-2', 'session-24-002', '강의 기초 메시지 2', '2026-08-01 09:00:02'),
    (UUID_TO_BIN(UUID()), 'sale-1', 'member-2', 'session-24-003', '강의 기초 메시지 3', '2026-08-01 09:00:03'),
    (UUID_TO_BIN(UUID()), 'sale-1', 'member-1', 'session-24-004', '강의 기초 메시지 4', '2026-08-01 09:00:04'),
    (UUID_TO_BIN(UUID()), 'sale-1', 'member-2', 'session-24-005', '강의 기초 메시지 5', '2026-08-01 09:00:05');

INSERT IGNORE INTO chat_read_states(room_id, member_id, last_read_id)
SELECT 'sale-1', 'member-1', id
FROM chat_messages WHERE room_id = 'sale-1' AND client_message_id = 'session-24-005';

INSERT IGNORE INTO chat_read_states(room_id, member_id, last_read_id)
SELECT 'sale-1', 'member-2', id
FROM chat_messages WHERE room_id = 'sale-1' AND client_message_id = 'session-24-002';
