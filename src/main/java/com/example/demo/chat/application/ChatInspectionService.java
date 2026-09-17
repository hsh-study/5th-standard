package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.ChatMessageRecorded;
import com.example.demo.chat.application.event.ChatInspectionFaults;
import com.example.demo.chat.domain.ChatInspectionResult;
import com.example.demo.chat.domain.ChatInspectionResultRepository;
import com.example.demo.chat.domain.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ChatInspectionService {
    private final ChatInspectionResultRepository repository;
    private final WordPolicyService policy;
    private final ChatInspectionFaults faults;
    private final PlatformTransactionManager transactionManager;

//    @Transactional
    public ChatInspectionResult inspect(ChatMessageRecorded event) {

        // PROCESS 가 실행되기 전 실패 주입
        faults.check(event.eventId(), ChatInspectionFaults.Point.BEFORE_PROCESS);

        // 트랜잭션 세팅
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);

        try {
            return transaction.execute(status ->
                repository.findByEventIdAndPolicyVersion(event.eventId(), "v1")
                    .orElseGet(() -> {
                        String terms = policy.matchedTerms(event.content());
                        ChatInspectionResult result = ChatInspectionResult.create(event, terms);

                        ChatInspectionResult saved = repository.save(result);
                        // DB 커밋 전 실패 주입
                        faults.check(event.eventId(), ChatInspectionFaults.Point.BEFORE_COMMIT);

                        return saved;
                    })
            );
        } catch (DataIntegrityViolationException ex) {
            // 실패 트랜잭션의 롤백이 끝난 뒤 새 트랜잭션으로 조회합니다.
            return transaction.execute(
                status ->
                    repository
                        .findByEventIdAndPolicyVersion(event.eventId(), "v1")
                        .orElseThrow(() -> ex));
        }
    }
}
