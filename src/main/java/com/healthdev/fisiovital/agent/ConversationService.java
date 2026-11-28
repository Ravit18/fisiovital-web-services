package com.healthdev.fisiovital.agent;

import com.healthdev.fisiovital.agent.dto.ConversationMessage;
import com.healthdev.fisiovital.agent.dto.ConversationSummary;
import com.healthdev.fisiovital.agent.memory.ChoicesLine;
import com.healthdev.fisiovital.exception.BusinessException;
import com.healthdev.fisiovital.model.Conversation;
import com.healthdev.fisiovital.model.MessageSender;
import com.healthdev.fisiovital.repository.ChatMessageRepository;
import com.healthdev.fisiovital.repository.ConversationRepository;
import com.healthdev.fisiovital.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Historial de chats con el asistente: cada usuario ve, retoma y borra solo sus conversaciones. */
@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final CurrentUserProvider currentUser;

    @Transactional(readOnly = true)
    public List<ConversationSummary> history() {
        return conversationRepository.findHistory(currentUser.get().userId()).stream()
                .map(c -> new ConversationSummary(c.getId(), titleOf(c), c.getStartedAt(), c.getLastActivityAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> messages(Long conversationId) {
        Conversation conversation = owned(conversationId);
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(conversation.getId()).stream()
                .map(m -> {
                    ChoicesLine.Parsed parsed = m.getSender() == MessageSender.ASSISTANT
                            ? ChoicesLine.parse(m.getContent()) : new ChoicesLine.Parsed(m.getContent(), List.of());
                    return new ConversationMessage(m.getId(), m.getSender().name(), parsed.text(), parsed.choices(),
                            m.getCreatedAt());
                })
                .toList();
    }

    @Transactional
    public void delete(Long conversationId) {
        Conversation conversation = owned(conversationId);
        chatMessageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    private Conversation owned(Long conversationId) {
        Long userId = currentUser.get().userId();
        return conversationRepository.findById(conversationId)
                .filter(c -> c.belongsTo(userId))
                .orElseThrow(() -> BusinessException.notFound("assistant.conversation.not.found"));
    }

    private String titleOf(Conversation conversation) {
        if (conversation.getTitle() != null && !conversation.getTitle().isBlank()) {
            return conversation.getTitle();
        }
        return chatMessageRepository
                .findFirstByConversationIdAndSenderOrderByCreatedAtAscIdAsc(conversation.getId(), MessageSender.USER)
                .map(m -> Conversation.titleOf(m.getContent()))
                .orElse("");
    }
}
