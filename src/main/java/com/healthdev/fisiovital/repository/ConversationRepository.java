package com.healthdev.fisiovital.repository;

import com.healthdev.fisiovital.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** Historial de chats del usuario (solo los que tienen mensajes), del mas reciente al mas antiguo. */
    @Query("""
            select c from Conversation c
            where c.user.id = :userId
              and exists (select 1 from ChatMessage m where m.conversation = c)
            order by c.lastActivityAt desc, c.id desc
            """)
    List<Conversation> findHistory(@Param("userId") Long userId);
}
