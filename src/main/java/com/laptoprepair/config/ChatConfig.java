package com.laptoprepair.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.memory.repository.jdbc.PostgresChatMemoryRepositoryDialect;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Configuration class for setting up chat-related beans, including the system
 * prompt, chat memory, and ChatClient.
 */
@Configuration
public class ChatConfig {
    @Value("classpath:system-prompt.txt")
    private Resource promptResource;

    @Value("${app.chat.max-user-messages}")
    private int maxUserMessages;

    private final JdbcTemplate jdbcTemplate;
    private final Environment environment;

    /**
     * Constructs a new ChatConfig with the given JdbcTemplate and Environment.
     * 
     * @param jdbcTemplate The JDBC template for database operations.
     * @param environment  The Spring environment for profile-specific
     *                     configurations.
     */
    public ChatConfig(JdbcTemplate jdbcTemplate, Environment environment) {
        this.jdbcTemplate = jdbcTemplate;
        this.environment = environment;
    }

    /**
     * Provides the system prompt as a String.
     * 
     * @return The content of the system prompt file.
     * @throws IOException If an error occurs while reading the prompt resource.
     */
    @Bean
    public String systemPrompt() throws IOException {
        return promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    /**
     * Configures and provides the ChatMemory bean.
     * Uses JdbcChatMemoryRepository with a Postgres dialect if the 'prod' profile
     * is active.
     * 
     * @return A configured ChatMemory instance.
     */
    @Bean
    public ChatMemory chatMemory() {
        JdbcChatMemoryRepository.Builder builder = JdbcChatMemoryRepository.builder()
                .jdbcTemplate(jdbcTemplate);

        // Configure dialect based on active profile
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equals(profile)) {
                builder.dialect(new PostgresChatMemoryRepositoryDialect());
                break;
            }
        }

        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(builder.build())
                .maxMessages(maxUserMessages * 3)
                .build();
    }

    /**
     * Configures and provides the ChatClient bean.
     * Sets the default system prompt and integrates with the chat memory.
     * 
     * @param builder      The ChatClient.Builder provided by Spring AI.
     * @param systemPrompt The system prompt string.
     * @param chatMemory   The chat memory instance.
     * @return A configured ChatClient instance.
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, String systemPrompt, ChatMemory chatMemory) {
        return builder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}