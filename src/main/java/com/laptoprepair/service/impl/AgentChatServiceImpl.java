package com.laptoprepair.service.impl;

import com.laptoprepair.service.AgentChatService;
import com.laptoprepair.service.SecurityService;
import com.laptoprepair.service.tools.DocumentSearchTools;
import com.laptoprepair.service.tools.ServiceItemTools;
import com.laptoprepair.service.tools.RequestTools;
import com.laptoprepair.service.tools.TimeTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of agent-based chat service with tool calling capabilities.
 * Uses Spring AI's ChatClient with tool callbacks and memory advisors.
 * The agent can make multiple tool calls sequentially during processing.
 */
@Service
public class AgentChatServiceImpl implements AgentChatService {

    private static final Logger logger = LoggerFactory.getLogger(AgentChatServiceImpl.class);

    private static final String MINIMAL_SYSTEM_PROMPT = "Bạn là trợ lý AI cho cửa hàng sửa chữa laptop tại Việt Nam. Hãy trả lời bằng tiếng Việt.";

    private final ChatClient agentChatClient;
    private final DocumentSearchTools documentSearchTools;
    private final ServiceItemTools serviceItemTools;
    private final RequestTools requestTools;
    private final TimeTools timeTools;
    private final SecurityService securityService;

    @Value("${laptoprepair.ai.chat.system-prompt-file:classpath:system-prompt.txt}")
    private String systemPromptFile;

    private String systemPrompt = MINIMAL_SYSTEM_PROMPT;

    public AgentChatServiceImpl(@Qualifier("agentChatClient") ChatClient agentChatClient,
            DocumentSearchTools documentSearchTools,
            ServiceItemTools serviceItemTools,
            RequestTools requestTools,
            TimeTools timeTools,
            SecurityService securityService) {
        this.agentChatClient = agentChatClient;
        this.documentSearchTools = documentSearchTools;
        this.serviceItemTools = serviceItemTools;
        this.requestTools = requestTools;
        this.timeTools = timeTools;
        this.securityService = securityService;
    }

    @PostConstruct
    public void initializeSystemPrompt() {
        // Load system prompt after dependency injection is complete
        try {
            if (systemPromptFile != null && systemPromptFile.startsWith("classpath:")) {
                String resourcePath = systemPromptFile.substring("classpath:".length());
                var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
                if (inputStream != null) {
                    systemPrompt = new String(inputStream.readAllBytes());
                } else {
                    logger.warn("System prompt resource not found: {}, using minimal prompt", resourcePath);
                    systemPrompt = MINIMAL_SYSTEM_PROMPT;
                }
            } else if (systemPromptFile != null) {
                systemPrompt = Files.readString(Path.of(systemPromptFile));
            } else {
                logger.warn("System prompt file not configured, using minimal prompt");
                systemPrompt = MINIMAL_SYSTEM_PROMPT;
            }
        } catch (IOException e) {
            logger.error("Failed to load system prompt from {}: {}", systemPromptFile, e.getMessage());
            systemPrompt = MINIMAL_SYSTEM_PROMPT;
        } catch (Exception e) {
            logger.error("Unexpected error loading system prompt: {}", e.getMessage(), e);
            systemPrompt = MINIMAL_SYSTEM_PROMPT;
        }
    }

    @Override
    public Flux<ChatResponse> processMessage(
            String conversationId,
            String userMessage,
            Authentication authentication) {

        return Flux.create(sink -> {
            try {
                String username = getUsername(authentication);
                List<Object> availableTools = getAvailableTools(authentication);

                logProcessingInfo(username, availableTools);

                ChatResponse chatResponse = executeChatRequest(
                        conversationId, userMessage, availableTools);

                emitResponse(chatResponse, sink);

            } catch (Exception e) {
                logger.error("Agent processing error: {}", e.getMessage(), e);
                sink.error(e);
            }
        });
    }

    private String getUsername(Authentication authentication) {
        return authentication != null ? authentication.getName() : "anonymous";
    }

    private List<Object> getAvailableTools(Authentication authentication) {
        List<Object> tools = new ArrayList<>();

        if (authentication != null) {
            // Public tools available to everyone
            tools.add(documentSearchTools);
            tools.add(serviceItemTools);
            tools.add(timeTools);

            // Staff-only tools
            if (securityService.hasRole(authentication, "ROLE_STAFF")) {
                tools.add(requestTools);
                logger.debug("Added staff tools for user: {}", authentication.getName());
            }
        }

        return tools;
    }

    private void logProcessingInfo(String username, List<Object> availableTools) {
        if (availableTools != null && username != null) {
            logger.info("Loaded {} tool(s) for user '{}': {}",
                    availableTools.size(),
                    username,
                    availableTools.stream()
                            .map(t -> t != null ? t.getClass().getSimpleName() : "null")
                            .toList());
        }
        logger.info("Processing message for user '{}' with multi-tool capability", username);
    }

    private ChatResponse executeChatRequest(String conversationId, String userMessage, List<Object> availableTools) {
        long startTime = System.currentTimeMillis();
        Object[] toolsArray = availableTools.toArray();

        logger.info("Calling ChatClient with {} tools registered", availableTools.size());

        // Prepare safe parameters
        String promptSystem = systemPrompt != null ? systemPrompt : MINIMAL_SYSTEM_PROMPT;
        String safeUserMessage = userMessage != null ? userMessage : "";
        Object[] safeToolsArray = toolsArray != null ? toolsArray : new Object[0];
        String safeConversationId = conversationId != null ? conversationId : "default";

        ChatResponse chatResponse = agentChatClient.prompt()
                .system(promptSystem)
                .user(safeUserMessage)
                .tools(safeToolsArray)
                .advisors(advisor -> advisor
                        .param(ChatMemory.CONVERSATION_ID, safeConversationId))
                .call()
                .chatResponse();

        if (chatResponse != null && chatResponse.getMetadata() != null) {
            logger.debug("Response metadata: {}", chatResponse.getMetadata());
        }

        long duration = System.currentTimeMillis() - startTime;
        logger.info("Agent processing completed in {}ms", duration);

        return chatResponse;
    }

    private void emitResponse(ChatResponse chatResponse, reactor.core.publisher.FluxSink<ChatResponse> sink) {
        if (chatResponse != null) {
            sink.next(chatResponse);
            sink.complete();
        } else {
            sink.error(new IllegalStateException("Chat response was null"));
        }
    }

}
