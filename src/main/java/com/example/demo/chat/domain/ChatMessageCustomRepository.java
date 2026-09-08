package com.example.demo.chat.domain;

import com.example.demo.chat.application.dto.ChatMessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;

import java.util.List;

public interface ChatMessageCustomRepository {

    Page<ChatMessageResponse> offsetPage(String roomId, int page, int size);

    Slice<ChatMessageResponse> offsetSlice(String roomId, int page, int size);

    List<ChatMessageResponse> history(String roomId, int size, long cursor);

    List<ChatMessageResponse> search(String roomId, long cursor, int size);
}
