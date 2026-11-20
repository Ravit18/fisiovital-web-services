package com.healthdev.fisiovital.agent.advisor;

import com.healthdev.fisiovital.agent.rag.KnowledgeBase;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * RAG (US17): busca en la base de conocimiento (Embeddings + VectorStore) los fragmentos mas parecidos a
 * la pregunta y los agrega al mensaje del usuario. Si no hay embeddings activos o la busqueda falla, el
 * asistente sigue funcionando sin contexto adicional.
 */
@Component
@RequiredArgsConstructor
public class KnowledgeAdvisor implements CallAdvisor {

    private final KnowledgeBase knowledgeBase;

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        List<String> fragments = knowledgeBase.semanticSearch(request.prompt().getUserMessage().getText());
        if (fragments.isEmpty()) {
            return chain.nextCall(request);
        }
        String context = String.join("\n", fragments.stream().map(f -> "- " + f).toList());
        ChatClientRequest augmented = request.mutate()
                .prompt(request.prompt().augmentUserMessage(user -> user.mutate()
                        .text(user.getText() + "\n\nInformacion de FisioVital que puede ayudarte "
                                + "(usala solo si es relevante):\n" + context)
                        .build()))
                .build();
        return chain.nextCall(augmented);
    }

    @Override
    public String getName() {
        return "KnowledgeAdvisor";
    }

    @Override
    public int getOrder() {
        return 200;
    }
}
