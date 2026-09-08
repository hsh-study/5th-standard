package com.example.demo.chat.application.dto;

import java.util.List;

public record CursorResponse<T> (
    List<T> items,
    Long nextCursor,
    boolean hasNext
) {
}
