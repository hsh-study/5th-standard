package com.example.demo.chat.domain;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatRoom {

    private final String id;
    private final Set<String> participantIds = ConcurrentHashMap.newKeySet();

    public ChatRoom(String id) {
        this.id = id;
    }

    public void join(String memberId) {
        participantIds.add(memberId);
    }

    public void leave(String memberId) {
        participantIds.remove(memberId);
    }

    public boolean canAccess(String memberId) {
        return participantIds.contains(memberId);
    }

    public String id() {
        return id;
    }

    public Set<String> participants() {
        return Collections.unmodifiableSet(participantIds);
    }
}

