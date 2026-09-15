package com.example.demo.chat.domain;

import com.example.demo.chat.application.dto.ChatMessageRecorded;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "chat_inspection_results",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_inspection_event_policy",
        columnNames = {"event_id","policy_version"}
    )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatInspectionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "room_id", nullable = false, length = 100)
    private String roomId;

    @Column(name = "policy_version", nullable = false, length=20)
    private String policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Verdict verdict;

    @Column(name = "matched_terms", nullable = false, length=1000)
    private String matchedTerms;

    @Column(name = "inspected_at", nullable = false)
    private Instant inspectedAt;

    private ChatInspectionResult(ChatMessageRecorded event, String terms) {
        eventId = event.eventId();
        messageId = event.messageId();
        roomId = event.roomId();
        policyVersion = "v1";
        verdict = terms.isEmpty() ? Verdict.PASS : Verdict.REVIEW_REQUIRED;
        matchedTerms = terms;
        inspectedAt = Instant.now();
    }

    public static ChatInspectionResult create(ChatMessageRecorded event, String terms) {
        return new ChatInspectionResult(event, terms);
    }

    public enum Verdict {
        PASS, REVIEW_REQUIRED
    }
}
