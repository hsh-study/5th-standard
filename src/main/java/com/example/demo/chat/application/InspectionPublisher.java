package com.example.demo.chat.application;

import com.example.demo.chat.application.dto.ChatMessageRecorded;

public interface InspectionPublisher {

    void publish(ChatMessageRecorded event);
}
