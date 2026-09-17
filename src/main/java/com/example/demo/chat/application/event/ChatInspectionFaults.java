package com.example.demo.chat.application.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** 오류를 발생시키기 위한 목적으로 만들어진 코드 입니다. */
@Component
public class ChatInspectionFaults {
    public enum Point {
        BEFORE_PROCESS,
        BEFORE_COMMIT,
        AFTER_COMMIT
    }

    private record Key(UUID eventId, Point point) {}

    private final ConcurrentHashMap<Key, AtomicInteger> remaining = new ConcurrentHashMap<>();
    private final boolean enabled;

    public ChatInspectionFaults(
        @Value("${kafka.inspection.lab-enabled:false}") boolean enabled,
        @Value("${kafka.inspection.fault-event-id:}") String eventId,
        @Value("${kafka.inspection.fault-point:BEFORE_PROCESS}") Point point,
        @Value("${kafka.inspection.fault-count:0}") int count) {

        this.enabled = enabled;
        if (!enabled) return;

        if (count > 0) {
            if (eventId.isBlank()) throw new IllegalArgumentException("실패할 event-id 가 필요합니다.");
            configureFailure(UUID.fromString(eventId), point, count);
        }
    }

    /** 실패 설정 */
    public void configureFailure(UUID eventId, Point point, int count) {
        if (!enabled) throw new IllegalStateException("검사 테스트 설정을 true 로 변경하세요.");

        remaining.put(new Key(eventId, point), new AtomicInteger(count));
    }

    public void clear(UUID id) {
        remaining.keySet().removeIf(key -> key.eventId().equals(id));
    }

    public void check(UUID id, Point point) {
        if (!enabled) return;
        Key key = new Key(id, point);
        var count = remaining.get(key);
        if (count == null) return;
        int previous = count.getAndUpdate(n -> Math.max(0, n - 1));
        if (previous == 1) remaining.remove(key, count);
        if (previous > 0) throw new IllegalStateException("주입된 실패 구역 : " + point + " eventId=" + id);
    }
}