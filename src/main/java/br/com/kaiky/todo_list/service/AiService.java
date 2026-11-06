package br.com.kaiky.todo_list.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import br.com.kaiky.todo_list.dto.TaskResponseDTO;
import br.com.kaiky.todo_list.entity.RecurrenceType;
import br.com.kaiky.todo_list.prompt.PromptInjectionAdvisor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openai.errors.BadRequestException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
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
    private final ObjectMapper objectMapper;
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile(
            "(?is).*<function[:(].*"
    );
    private static final Pattern MALFORMED_TOOL_CALL_PATTERN = Pattern.compile(
            "(?is).*attempted to call tool '([A-Za-z0-9_]+)=(\\{.*})' which was not in request\\.tools.*"
    );
    private static final Pattern JSON_OBJECT_PATTERN = Pattern.compile(
            "(?s)\\{.*}"
    );
    private static final DateTimeFormatter CURRENT_TIME_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd HH:mm"
    );

    public AiService(
            ChatClient.Builder chatClientBuilder,
            OpenAiChatModel chatModel,
            TaskTools taskTools,
            PromptService promptService,
            PromptInjectionAdvisor promptInjectionAdvisor,
            RedisChatMemoryRepository redisChatMemoryRepository,
            ChatHistoryService chatHistoryService
    ) {
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(redisChatMemoryRepository)
                .maxMessages(20)
                .build();

        this.chatClient = chatClientBuilder
                // Groq rejects reasoning_content when tool messages are sent back.
                .defaultOptions(chatModel.getOptions().mutate()
                        .extraBody(Map.of("include_reasoning", false)))
                .defaultAdvisors(
                        promptInjectionAdvisor,
                        MessageChatMemoryAdvisor.builder(chatMemory).build()
                )
                .build();

        this.taskTools = taskTools;
        this.promptService = promptService;
        this.chatHistoryService = chatHistoryService;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();

    }

    public AiChatResponseDTO chat(AiChatRequestDTO dto) {
        UUID conversationId = dto.conversationId() != null
                ? dto.conversationId()
                : UUID.randomUUID();

        String response;
        try {
            response = chatClient
                    .prompt()
                    .system(promptService.getSystemPrompt() + currentDateTimeInstruction())
                    .user(dto.message())
                    .advisors(advisor -> advisor.param(
                            ChatMemory.CONVERSATION_ID,
                            conversationId.toString()
                    ))
                    .tools(taskTools)
                    .call()
                    .content();
        } catch (BadRequestException exception) {
            Optional<String> recoveredResponse = recoverMalformedToolCall(exception)
                    .or(() -> recoverTaskOperationFromUserMessage(dto.message(), conversationId));
            if (recoveredResponse.isEmpty()) {
                throw exception;
            }
            response = recoveredResponse.get();
        }

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

    private Optional<String> recoverMalformedToolCall(BadRequestException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return Optional.empty();
        }

        var matcher = MALFORMED_TOOL_CALL_PATTERN.matcher(message);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String toolName = matcher.group(1);
        String argumentsJson = matcher.group(2);

        try {
            JsonNode arguments = objectMapper.readTree(argumentsJson);
            TaskResponseDTO task = executeRecoveredToolCall(toolName, arguments);
            return Optional.of(formatTaskOperationResponse(toolName, task));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private Optional<String> recoverTaskOperationFromUserMessage(String userMessage, UUID conversationId) {
        try {
            String extraction = chatClient
                    .prompt()
                    .system("""
                            You extract exactly one task operation from the user's message.

                            Return only one JSON object. Do not use Markdown.

                            Supported operations:
                            - createTask
                            - createTaskWithReminder
                            - createRecurringTask
                            - createRecurringTaskWithReminder
                            - updateTaskStatus
                            - markTaskAsCompletedByTitle
                            - markTaskAsPendingByTitle
                            - deleteTask
                            - deleteTaskByTitle

                            JSON shape:
                            {
                              "operation": "createTask",
                              "title": "Task title",
                              "description": "Task description",
                              "id": "task UUID when needed",
                              "completed": true,
                              "dueAt": "ISO local date-time when needed",
                              "recurrenceType": "NONE, DAILY, WEEKLY, or MONTHLY"
                            }

                            Rules:
                            - Current date and time: %s.
                            - Current timezone: %s.
                            - Resolve relative dates using the current date above.
                            - "today" means the current date above.
                            - "tomorrow" means the day after the current date above.
                            - If the user gives only a time for a reminder, use the current date when they say today, otherwise ask for clarification by returning {"operation":"none"}.
                            - If the user asks to create a task, the operation is createTask unless they ask for a reminder or recurrence.
                            - Creating a task requires a title, but never requires an existing id.
                            - If the user asks you to create the description, write a concise useful description.
                            - Use empty string for missing optional description.
                            - If the user did not request a valid task operation, return {"operation":"none"}.
                            """.formatted(currentDateTime(), ZoneId.systemDefault()))
                    .user(userMessage)
                    .advisors(advisor -> advisor.param(
                            ChatMemory.CONVERSATION_ID,
                            conversationId + ":operation-extraction"
                    ))
                    .call()
                    .content();

            JsonNode operation = parseJsonObject(extraction);
            String toolName = optionalText(operation, "operation");
            if (toolName.isBlank() || "none".equalsIgnoreCase(toolName)) {
                return Optional.empty();
            }

            TaskResponseDTO task = executeRecoveredToolCall(toolName, operation);
            return Optional.of(formatTaskOperationResponse(toolName, task));
        } catch (Exception ignored) {
            return Optional.of("""
                    I could not complete that task operation.

                    Please try again by naming the task clearly or providing its ID.
                    """);
        }
    }

    private TaskResponseDTO executeRecoveredToolCall(String toolName, JsonNode arguments) {
        return switch (toolName) {
            case "createTask" -> taskTools.createTask(
                    requiredText(arguments, "title"),
                    optionalText(arguments, "description")
            );
            case "createTaskWithReminder" -> taskTools.createTaskWithReminder(
                    requiredText(arguments, "title"),
                    optionalText(arguments, "description"),
                    requiredDateTime(arguments, "dueAt")
            );
            case "createRecurringTask" -> taskTools.createRecurringTask(
                    requiredText(arguments, "title"),
                    optionalText(arguments, "description"),
                    recurrenceType(arguments)
            );
            case "createRecurringTaskWithReminder" -> taskTools.createRecurringTaskWithReminder(
                    requiredText(arguments, "title"),
                    optionalText(arguments, "description"),
                    requiredDateTime(arguments, "dueAt"),
                    recurrenceType(arguments)
            );
            case "updateTaskStatus" -> taskTools.updateTaskStatus(
                    requiredUuid(arguments, "id"),
                    requiredBoolean(arguments, "completed")
            );
            case "markTaskAsCompletedByTitle" -> taskTools.markTaskAsCompletedByTitle(
                    requiredText(arguments, "title")
            );
            case "markTaskAsPendingByTitle" -> taskTools.markTaskAsPendingByTitle(
                    requiredText(arguments, "title")
            );
            case "deleteTask" -> taskTools.deleteTask(
                    requiredUuid(arguments, "id")
            );
            case "deleteTaskByTitle" -> taskTools.deleteTaskByTitle(
                    requiredText(arguments, "title")
            );
            default -> throw new IllegalArgumentException("Unsupported recovered tool call.");
        };
    }

    private String formatTaskOperationResponse(String toolName, TaskResponseDTO task) {
        String status = task.completed() ? "Completed" : "Pending";
        String description = task.description() == null || task.description().isBlank()
                ? ""
                : "\n**Description:** " + task.description();
        String reminder = task.dueAt() == null
                ? ""
                : "\n**Reminder:** " + task.dueAt();
        String recurrence = task.recurrenceType() == null || task.recurrenceType() == RecurrenceType.NONE
                ? ""
                : "\n**Repeats:** " + task.recurrenceType().name().toLowerCase();

        if (toolName.startsWith("deleteTask")) {
            return """
                    Task deleted successfully.

                    **Title:** %s
                    """.formatted(task.title());
        }

        return """
                Task operation completed successfully.

                **Title:** %s%s%s%s
                **Status:** %s
                """.formatted(task.title(), description, reminder, recurrence, status);
    }

    private String requiredText(JsonNode arguments, String fieldName) {
        String value = optionalText(arguments, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }

        return value;
    }

    private String optionalText(JsonNode arguments, String fieldName) {
        JsonNode value = arguments.get(fieldName);
        if (value == null || value.isNull()) {
            return "";
        }

        return value.asText("");
    }

    private UUID requiredUuid(JsonNode arguments, String fieldName) {
        return UUID.fromString(requiredText(arguments, fieldName));
    }

    private boolean requiredBoolean(JsonNode arguments, String fieldName) {
        JsonNode value = arguments.get(fieldName);
        if (value == null || value.isNull()) {
            throw new IllegalArgumentException("Missing required field: " + fieldName);
        }

        return value.asBoolean();
    }

    private LocalDateTime requiredDateTime(JsonNode arguments, String fieldName) {
        return LocalDateTime.parse(requiredText(arguments, fieldName));
    }

    private RecurrenceType recurrenceType(JsonNode arguments) {
        String value = requiredText(arguments, "recurrenceType");
        return RecurrenceType.valueOf(value.toUpperCase());
    }

    private String currentDateTimeInstruction() {
        return """

                ## Current Date And Time

                Current date and time: %s.
                Current timezone: %s.

                Use this date for relative expressions like today, tomorrow, next week, this morning, tonight, hoje, amanhã, semana que vem, hoje às 10:56, and similar requests.
                Never use training-data dates for reminders or schedules.
                When the user gives a reminder date/time, convert it to ISO local date-time format before calling a tool.
                """.formatted(currentDateTime(), ZoneId.systemDefault());
    }

    private String currentDateTime() {
        return LocalDateTime.now().format(CURRENT_TIME_FORMATTER);
    }

    private JsonNode parseJsonObject(String value) throws Exception {
        String normalizedValue = value == null ? "" : value.trim();
        var matcher = JSON_OBJECT_PATTERN.matcher(normalizedValue);
        if (!matcher.find()) {
            throw new IllegalArgumentException("No JSON object was returned.");
        }

        return objectMapper.readTree(matcher.group());
    }
}
