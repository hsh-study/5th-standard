package com.example.demo.chat.application.dto;

import java.util.List;

public record HistoryResponse<T> (
    List<T> items,
    Long beforeCursor,
    boolean hasPrevious
) {
}
