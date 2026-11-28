package com.healthdev.fisiovital.repository;

import com.healthdev.fisiovital.model.ChatMessage;
import com.healthdev.fisiovital.model.MessageSender;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Ultimos mensajes de la conversacion, del mas reciente al mas antiguo. */
    List<ChatMessage> findByConversationIdOrderByCreatedAtDescIdDesc(Long conversationId, Pageable pageable);

    /** Conversacion completa en orden de lectura. */
    List<ChatMessage> findByConversationIdOrderByCreatedAtAscIdAsc(Long conversationId);

    /** Primer mensaje del usuario: da el titulo de las conversaciones creadas antes de guardar titulos. */
    Optional<ChatMessage> findFirstByConversationIdAndSenderOrderByCreatedAtAscIdAsc(Long conversationId,
                                                                                    MessageSender sender);

    void deleteByConversationId(Long conversationId);
}
