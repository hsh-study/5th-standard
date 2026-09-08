package com.example.demo.chat.api.response;

import java.util.List;

public record OffsetPageResponse<T>(
    List<T> items,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext,
    Integer nextPage
) {
}
