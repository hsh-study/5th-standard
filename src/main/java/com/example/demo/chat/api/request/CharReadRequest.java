package com.example.demo.chat.api.request;

import jakarta.validation.constraints.Min;

public record CharReadRequest(
    @Min(0)
    long lastReadId
) {
}
