package com.example.demo.chat.infra;

import com.example.demo.chat.application.dto.ChatMessageResponse;
import com.example.demo.chat.domain.ChatMessageCustomRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.demo.chat.domain.QChatMessage.chatMessage;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;

    /**
     * Page (PageImpl) 을 이용한 조회 결과
     */
    @Override
    public Page<ChatMessageResponse> offsetPage(String roomId, int page, int size) {
        PageRequest request = makeOffsetRequest(roomId, page, size);

        List<ChatMessageResponse> items = jpaQueryFactory.select(
            Projections.constructor(ChatMessageResponse.class,
                chatMessage.id,
                chatMessage.messageId,
                chatMessage.roomId,
                chatMessage.senderId,
                chatMessage.clientMessageId,
                chatMessage.content,
                chatMessage.sentAt
                ))
            .from(chatMessage)
            .where(chatMessage.roomId.eq(roomId))
            .orderBy(chatMessage.id.asc())
            .offset(request.getOffset())
            .limit(request.getPageSize())
            .fetch();

        Long totalCount = jpaQueryFactory.select(chatMessage.count())
            .from(chatMessage)
            .where(chatMessage.roomId.eq(roomId))
            .fetchOne();

        return new PageImpl<>(items, request, totalCount == null ? 0 : totalCount);
    }

    /**
     * Slice (SliceImpl) 을 이용한 조회 결과
     * JPA 에서는 Slice 를 이용한 조회는 offset 과 limit 을 사용하여 조회한 후, 조회된 데이터의 개수가 limit 보다 크면 다음 페이지가 존재한다고 판단하여 true 를 반환한다.
     * Querydsl 에서는 Slice 를 이용한 조회는 offset 과 limit + 1 을 사용하여 조회한 후,
     * 조회된 데이터의 개수가 limit 보다 크면 다음 페이지가 존재한다고 판단하여 true 를 반환하도록 코드를 작성해야 한다.
     */
    @Override
    public Slice<ChatMessageResponse> offsetSlice(String roomId, int page, int size) {
        PageRequest request = makeOffsetRequest(roomId, page, size);

        List<ChatMessageResponse> fetched = jpaQueryFactory.select(
                Projections.constructor(ChatMessageResponse.class,
                    chatMessage.id,
                    chatMessage.messageId,
                    chatMessage.roomId,
                    chatMessage.senderId,
                    chatMessage.clientMessageId,
                    chatMessage.content,
                    chatMessage.sentAt
                ))
            .from(chatMessage)
            .where(chatMessage.roomId.eq(roomId))
            .orderBy(chatMessage.id.asc())
            .offset(request.getOffset())
            .limit(request.getPageSize() + 1)
            .fetch();

        boolean hasNext = fetched.size() > size;
        List<ChatMessageResponse> items = List.copyOf(fetched.subList(0, Math.min(size, fetched.size())));

        return new SliceImpl<>(items, request, hasNext);
    }

    /**
     * 채팅방의 과거 메시지 내역을 최신 순으로 size 만큼 가져온다.
     * TODO - beforeCursor 처리
     */
    @Override
    public List<ChatMessageResponse> history(String roomId, int size, long cursor) {

        BooleanBuilder predicates = new BooleanBuilder();
        predicates.and(chatMessage.roomId.eq(roomId));
        predicates.and(cursor > 0 ? chatMessage.id.lt(cursor) : null);

        return jpaQueryFactory.select(
                Projections.constructor(ChatMessageResponse.class,
                    chatMessage.id,
                    chatMessage.messageId,
                    chatMessage.roomId,
                    chatMessage.senderId,
                    chatMessage.clientMessageId,
                    chatMessage.content,
                    chatMessage.sentAt
                ))
            .from(chatMessage)
            .where(predicates)
            .orderBy(chatMessage.id.desc())
            .limit(size + 1)
            .fetch();
    }

    @Override
    public List<ChatMessageResponse> search(String roomId, long cursor, int size) {

        BooleanBuilder predicates = new BooleanBuilder();
        predicates.and(chatMessage.roomId.eq(roomId));
        predicates.and(chatMessage.id.gt(cursor));

        return jpaQueryFactory.select(
                Projections.constructor(ChatMessageResponse.class,
                    chatMessage.id,
                    chatMessage.messageId,
                    chatMessage.roomId,
                    chatMessage.senderId,
                    chatMessage.clientMessageId,
                    chatMessage.content,
                    chatMessage.sentAt
                ))
            .from(chatMessage)
            .where(predicates)
            .orderBy(chatMessage.id.desc())
            .limit(size + 1)
            .fetch();
    }

    private PageRequest makeOffsetRequest(String roomId, int page, int size) {
        if (!hasText(roomId)) {
            throw new IllegalArgumentException("roomId 가 필요 합니다.");
        } else if (page < 0) {
            throw new IllegalArgumentException("page 는 0 이상만 허용 됩니다.");
        } else if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size 는 1 이상, 100 이하만 허용 됩니다.");
        }

        return PageRequest.of(page, size);
    }

    private boolean hasText(String roomId) {
        return roomId != null && !roomId.isBlank();
    }
}
