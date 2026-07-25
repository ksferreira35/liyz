package br.com.kaiky.todo_list.service;

import java.util.UUID;
import java.util.regex.Pattern;

import br.com.kaiky.todo_list.prompt.PromptInjectionAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import br.com.kaiky.todo_list.dto.AiChatRequestDTO;
import br.com.kaiky.todo_list.dto.AiChatResponseDTO;
import br.com.kaiky.todo_list.prompt.PromptService;
import br.com.kaiky.todo_list.repository.RedisChatMemoryRepository;
import br.com.kaiky.todo_list.tools.TaskTools;

@Service
public class AiService {

    private final ChatClient chatClient;
    private final TaskTools taskTools;
    private final PromptService promptService;
    private final ChatHistoryService chatHistoryService;
    private final UUID defaultConversationId;
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile(
            "(?is).*<function[:(].*"
    );

    public AiService(
            ChatClient.Builder chatClientBuilder,
            TaskTools taskTools,
            PromptService promptService,
            PromptInjectionAdvisor promptInjectionAdvisor,
            RedisChatMemoryRepository redisChatMemoryRepository,
            ChatHistoryService chatHistoryService,
            @Value("${app.chat.default-conversation-id}") String defaultConversationId
    ) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(20)
                .build();

        this.chatClient = chatClientBuilder
                .defaultAdvisors(
                        promptInjectionAdvisor,
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();

        this.taskTools = taskTools;
        this.promptService = promptService;
        this.chatHistoryService = chatHistoryService;
        this.defaultConversationId = UUID.fromString(defaultConversationId);

    }

    public AiChatResponseDTO chat(AiChatRequestDTO dto) {
        UUID conversationId = dto.conversationId() != null
                ? dto.conversationId()
                : defaultConversationId;

        String response = chatClient
                .prompt()
                .system(promptService.getSystemPrompt())
                .user(dto.message())
                .advisors(advisor -> advisor.param(
                        ChatMemory.CONVERSATION_ID,
                        conversationId.toString()
                ))
                .tools(taskTools)
                .call()
                .content();
        String assistantResponse = sanitizeAssistantResponse(response);

        return saveAndReturn(conversationId, dto.message(), assistantResponse);
    }

    private AiChatResponseDTO saveAndReturn(
            UUID conversationId,
            String userMessage,
            String assistantResponse
    ) {
        chatHistoryService.getOrCreateConversation(conversationId, userMessage);
        chatHistoryService.saveUserMessage(conversationId, userMessage);
        chatHistoryService.saveAssistantMessage(conversationId, assistantResponse);

        return new AiChatResponseDTO(conversationId, assistantResponse);
    }

    private String sanitizeAssistantResponse(String response) {
        String assistantResponse = response == null ? "" : response;

        if (FUNCTION_CALL_PATTERN.matcher(assistantResponse).matches()) {
            return """
                    I could not complete that task operation.

                    Please try again by naming the task clearly or providing its ID.
                    """;
        }

        return assistantResponse;
    }
}
