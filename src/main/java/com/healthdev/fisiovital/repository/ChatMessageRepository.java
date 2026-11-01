package com.healthdev.fisiovital.repository;

import com.healthdev.fisiovital.model.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /** Ultimos mensajes de la conversacion, del mas reciente al mas antiguo. */
    List<ChatMessage> findByConversationIdOrderByCreatedAtDescIdDesc(Long conversationId, Pageable pageable);
}
