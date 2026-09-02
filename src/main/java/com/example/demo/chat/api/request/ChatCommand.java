package com.example.demo.chat.api.request;

import jakarta.validation.constraints.NotBlank;

public record ChatCommand(
    @NotBlank String clientMessageId, @NotBlank String content
) {
}
