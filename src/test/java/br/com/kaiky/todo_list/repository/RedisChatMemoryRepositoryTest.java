package br.com.kaiky.todo_list.repository;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class RedisChatMemoryRepositoryTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private RedisChatMemoryRepository repository;

    @BeforeEach
    @SuppressWarnings({"unchecked", "unused"})
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        repository = new RedisChatMemoryRepository(redisTemplate, 7);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void shouldReturnConversationIdsFromRedisKeys() {
        when(redisTemplate.keys("chat:memory:*"))
                .thenReturn(Set.of(
                        "chat:memory:first-id",
                        "chat:memory:second-id"
                ));

        List<String> conversationIds = repository.findConversationIds();

        assertThat(conversationIds)
                .containsExactlyInAnyOrder("first-id", "second-id");
    }

    @Test
    void shouldReturnEmptyConversationIdsWhenRedisReturnsNoKeys() {
        when(redisTemplate.keys("chat:memory:*")).thenReturn(null);

        assertThat(repository.findConversationIds()).isEmpty();
    }

    @Test
    void shouldReturnEmptyMessagesWhenConversationDoesNotExist() {
        when(valueOperations.get("chat:memory:missing")).thenReturn(null);

        assertThat(repository.findByConversationId("missing")).isEmpty();
    }

    @Test
    void shouldReadStoredMessagesFromRedis() {
        when(valueOperations.get("chat:memory:conversation-id"))
                .thenReturn("""
                        [
                          {"type":"SYSTEM","text":"System rules"},
                          {"type":"USER","text":"Meu nome é Jorge"},
                          {"type":"ASSISTANT","text":"Olá Jorge"}
                        ]
                        """);

        List<Message> messages = repository.findByConversationId("conversation-id");

        assertThat(messages).hasSize(3);
        assertThat(messages.get(0).getMessageType()).isEqualTo(MessageType.SYSTEM);
        assertThat(messages.get(0).getText()).isEqualTo("System rules");
        assertThat(messages.get(1).getMessageType()).isEqualTo(MessageType.USER);
        assertThat(messages.get(1).getText()).isEqualTo("Meu nome é Jorge");
        assertThat(messages.get(2).getMessageType()).isEqualTo(MessageType.ASSISTANT);
        assertThat(messages.get(2).getText()).isEqualTo("Olá Jorge");
    }

    @Test
    void shouldSaveMessagesToRedisWithTtl() {
        List<Message> messages = List.of(
                new SystemMessage("System rules"),
                new UserMessage("Meu nome é Jorge"),
                new AssistantMessage("Olá Jorge")
        );

        repository.saveAll("conversation-id", messages);

        verify(valueOperations).set(
                eq("chat:memory:conversation-id"),
                eq("""
                        [{"type":"SYSTEM","text":"System rules"},{"type":"USER","text":"Meu nome é Jorge"},{"type":"ASSISTANT","text":"Olá Jorge"}]"""
                        .stripIndent()
                        .trim()),
                eq(Duration.ofDays(7))
        );
    }

    @Test
    void shouldDeleteConversationMemory() {
        repository.deleteByConversationId("conversation-id");

        verify(redisTemplate).delete("chat:memory:conversation-id");
    }
}
