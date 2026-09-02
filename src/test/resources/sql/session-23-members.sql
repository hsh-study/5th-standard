DELETE FROM member_roles;
DELETE FROM members;

INSERT INTO members(member_id, display_name, encoded_password, enabled) VALUES
    ('member-1', '회원 1', '$2a$10$SubrMobMIz91R4Fwux2wI.FgIE2bqBqSHl2XzhAk3nhM9VojazXu.', TRUE),
    ('member-2', '회원 2', '$2a$10$SubrMobMIz91R4Fwux2wI.FgIE2bqBqSHl2XzhAk3nhM9VojazXu.', TRUE),
    ('admin', '관리자', '$2a$10$SubrMobMIz91R4Fwux2wI.FgIE2bqBqSHl2XzhAk3nhM9VojazXu.', TRUE);

INSERT INTO member_roles(member_id, role) VALUES
    ('member-1', 'MEMBER'),
    ('member-2', 'MEMBER'),
    ('admin', 'ADMIN'),
    ('admin', 'MEMBER');
