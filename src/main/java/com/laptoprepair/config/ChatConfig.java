package com.laptoprepair.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.NonNull;

import lombok.RequiredArgsConstructor;

/**
 * Configuration class for Spring AI chat components
 */
@Configuration
@RequiredArgsConstructor
public class ChatConfig {

    private final @NonNull JdbcTemplate jdbcTemplate;

    @Value("${laptoprepair.ai.chat.max-user-messages:10}")
    private int maxUserMessages;

    @Bean
    public ChatMemory chatMemory() {
        JdbcChatMemoryRepository repository = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate)
                .dialect(new PostgresChatMemoryRepositoryDialect())
                .build();

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(maxUserMessages * 3)
                .build();
    }

    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter(1000, 400, 5, 10000, true);
    }

    @Bean
    public ChatClient agentChatClient(ChatClient.Builder builder, @NonNull ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}