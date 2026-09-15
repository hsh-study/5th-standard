package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.ChatMessageRecorded;
import com.example.demo.chat.domain.ChatInspectionResult;
import com.example.demo.chat.domain.ChatInspectionResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatInspectionService {
    private final ChatInspectionResultRepository repository;
    private final WordPolicyService policy;

    @Transactional
    public ChatInspectionResult inspect(ChatMessageRecorded event) {

        String terms = policy.matchedTerms(event.content());
        ChatInspectionResult result = ChatInspectionResult.create(event, terms);

        return repository.save(result);
    }
}
