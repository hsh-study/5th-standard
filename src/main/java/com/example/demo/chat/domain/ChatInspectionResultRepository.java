package com.example.demo.chat.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.UUID;

public interface ChatInspectionResultRepository extends JpaRepository<ChatInspectionResult, Long> {
    Optional<ChatInspectionResult> findByEventIdAndPolicyVersion(UUID eventId, String policyVersion);
}
