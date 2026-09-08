package com.example.demo.chat.api.response;

import java.util.List;

public record OffsetSliceResponse<T>(
    List<T> items,
    int page,
    int size,
    boolean hasNext,
    Integer nextPage
) {
}
