package br.com.kaiky.todo_list.service;

import br.com.kaiky.todo_list.entity.ChatConversation;
import br.com.kaiky.todo_list.entity.ChatMessage;
import br.com.kaiky.todo_list.repository.ChatConversationRepository;
import br.com.kaiky.todo_list.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceTest {

    @Mock
    private ChatConversationRepository chatConversationRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @InjectMocks
    private ChatHistoryService chatHistoryService;

    @Test
    void shouldReturnExistingConversation() {
        UUID conversationId = UUID.randomUUID();
        ChatConversation existingConversation = new ChatConversation(
                conversationId,
                "Existing conversation"
        );

        when(chatConversationRepository.findById(conversationId))
                .thenReturn(Optional.of(existingConversation));

        ChatConversation result = chatHistoryService.getOrCreateConversation(
                conversationId,
                "Hello"
        );

        assertThat(result).isSameAs(existingConversation);
        verifyNoInteractions(chatMessageRepository);
    }

    @Test
    void shouldCreateConversationWithTitleFromFirstMessage() {
        UUID conversationId = UUID.randomUUID();

        when(chatConversationRepository.findById(conversationId))
                .thenReturn(Optional.empty());
        when(chatConversationRepository.save(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatConversation result = chatHistoryService.getOrCreateConversation(
                conversationId,
                "  Meu nome é Jorge  "
        );

        assertThat(result.getId()).isEqualTo(conversationId);
        assertThat(result.getTitle()).isEqualTo("Meu nome é Jorge");
    }

    @Test
    void shouldCreateConversationWithFallbackTitleWhenMessageIsBlank() {
        UUID conversationId = UUID.randomUUID();

        when(chatConversationRepository.findById(conversationId))
                .thenReturn(Optional.empty());
        when(chatConversationRepository.save(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatConversation result = chatHistoryService.getOrCreateConversation(
                conversationId,
                "   "
        );

        assertThat(result.getTitle()).isEqualTo("New conversation");
    }

    @Test
    void shouldTruncateLongConversationTitle() {
        UUID conversationId = UUID.randomUUID();
        String longMessage = "a".repeat(100);

        when(chatConversationRepository.findById(conversationId))
                .thenReturn(Optional.empty());
        when(chatConversationRepository.save(any(ChatConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ChatConversation result = chatHistoryService.getOrCreateConversation(
                conversationId,
                longMessage
        );

        assertThat(result.getTitle()).hasSize(80);
        assertThat(result.getTitle()).endsWith("...");
    }

    @Test
    void shouldSaveUserMessage() {
        UUID conversationId = UUID.randomUUID();
        ChatConversation conversation = new ChatConversation(conversationId, "Hello");

        when(chatConversationRepository.findById(conversationId))
                .thenReturn(Optional.of(conversation));

        chatHistoryService.saveUserMessage(conversationId, "Hello");

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());

        assertThat(messageCaptor.getValue().getConversation()).isSameAs(conversation);
        assertThat(messageCaptor.getValue().getRole()).isEqualTo(ChatMessage.Role.USER);
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("Hello");
    }

    @Test
    void shouldSaveAssistantMessage() {
        UUID conversationId = UUID.randomUUID();
        ChatConversation conversation = new ChatConversation(conversationId, "Hello");

        when(chatConversationRepository.findById(conversationId))
                .thenReturn(Optional.of(conversation));

        chatHistoryService.saveAssistantMessage(conversationId, "Hi!");

        ArgumentCaptor<ChatMessage> messageCaptor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(chatMessageRepository).save(messageCaptor.capture());

        assertThat(messageCaptor.getValue().getConversation()).isSameAs(conversation);
        assertThat(messageCaptor.getValue().getRole()).isEqualTo(ChatMessage.Role.ASSISTANT);
        assertThat(messageCaptor.getValue().getContent()).isEqualTo("Hi!");
    }

    @Test
    void shouldThrowWhenSavingMessageForMissingConversation() {
        UUID conversationId = UUID.randomUUID();

        when(chatConversationRepository.findById(conversationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatHistoryService.saveUserMessage(conversationId, "Hello"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(conversationId.toString());
        verifyNoInteractions(chatMessageRepository);
    }
}
