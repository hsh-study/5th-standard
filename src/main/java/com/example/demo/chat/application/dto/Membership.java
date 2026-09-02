package com.example.demo.chat.application.dto;

import java.util.Set;

public record Membership(
    String liveSaleId, String memberId, Set<String> participants) {
}
