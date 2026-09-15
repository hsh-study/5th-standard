package com.example.demo.chat.application;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class WordPolicyService {
    public String matchedTerms(String content) {

        String normalized = content.toLowerCase(Locale.ROOT);
        return List.of("금칙어예시", "spam-link")
            .stream()
            .filter(normalized::contains)
            .collect(Collectors.joining(","));
    }
}
