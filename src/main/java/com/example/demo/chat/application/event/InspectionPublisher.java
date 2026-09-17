package com.example.demo.chat.application.event;

import com.example.demo.chat.application.dto.ChatMessageRecorded;

public interface InspectionPublisher {

    void publish(ChatMessageRecorded event);
}
