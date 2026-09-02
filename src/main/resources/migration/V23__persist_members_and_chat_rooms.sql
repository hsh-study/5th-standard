CREATE TABLE IF NOT EXISTS members (
    member_id VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    encoded_password VARCHAR(100) NOT NULL,
    enabled BIT NOT NULL,
    PRIMARY KEY (member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS member_roles (
    member_id VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL,
    CONSTRAINT uk_member_role UNIQUE (member_id, role),
    CONSTRAINT fk_member_roles_member
        FOREIGN KEY (member_id) REFERENCES members(member_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_rooms (
    room_id VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_participants (
    id BIGINT NOT NULL AUTO_INCREMENT,
    room_id VARCHAR(100) NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_participant_room_member UNIQUE (room_id, member_id),
    CONSTRAINT fk_chat_participant_room
        FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id),
    CONSTRAINT fk_chat_participant_member
        FOREIGN KEY (member_id) REFERENCES members(member_id),
    INDEX idx_chat_participant_member (member_id, room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BINARY(16) NOT NULL,
    room_id VARCHAR(100) NOT NULL,
    sender_id VARCHAR(100) NOT NULL,
    client_message_id VARCHAR(100) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    sent_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_message_id UNIQUE (message_id),
    CONSTRAINT uk_chat_message_room_client UNIQUE (room_id, client_message_id),
    CONSTRAINT fk_chat_message_room
        FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id),
    CONSTRAINT fk_chat_message_sender
        FOREIGN KEY (sender_id) REFERENCES members(member_id),
    INDEX idx_chat_message_room_sequence (room_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO members(member_id, display_name, encoded_password, enabled) VALUES
    ('member-1', '회원 1', '$2a$10$SubrMobMIz91R4Fwux2wI.FgIE2bqBqSHl2XzhAk3nhM9VojazXu.', b'1'),
    ('member-2', '회원 2', '$2a$10$SubrMobMIz91R4Fwux2wI.FgIE2bqBqSHl2XzhAk3nhM9VojazXu.', b'1'),
    ('admin', '관리자', '$2a$10$SubrMobMIz91R4Fwux2wI.FgIE2bqBqSHl2XzhAk3nhM9VojazXu.', b'1');

INSERT IGNORE INTO member_roles(member_id, role) VALUES
    ('member-1', 'MEMBER'),
    ('member-2', 'MEMBER'),
    ('admin', 'ADMIN'),
    ('admin', 'MEMBER');
