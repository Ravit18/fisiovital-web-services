package com.healthdev.fisiovital.agent.advisor;

import com.healthdev.fisiovital.agent.rag.KnowledgeBase;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** RAG (US17): el advisor agrega al mensaje los fragmentos encontrados y tolera fallas del VectorStore. */
class KnowledgeAdvisorTest {

    private final VectorStore store = mock(VectorStore.class);
    private final CallAdvisorChain chain = mock(CallAdvisorChain.class);

    @SuppressWarnings("unchecked")
    private KnowledgeAdvisor advisor(String apiKey) {
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(store);
        return new KnowledgeAdvisor(new KnowledgeBase(provider, apiKey));
    }

    private ChatClientRequest request(String text) {
        return new ChatClientRequest(new Prompt(text), Map.of());
    }

    @Test
    void addsRetrievedContextToTheUserMessage() {
        when(store.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("Cada sesion dura 60 minutos.")));
        when(chain.nextCall(any())).thenReturn(mock(ChatClientResponse.class));

        advisor("key").adviseCall(request("cuanto dura una sesion?"), chain);

        ArgumentCaptor<ChatClientRequest> sent = ArgumentCaptor.forClass(ChatClientRequest.class);
        verify(chain).nextCall(sent.capture());
        String text = sent.getValue().prompt().getUserMessage().getText();
        assertThat(text).startsWith("cuanto dura una sesion?").contains("Cada sesion dura 60 minutos.");
        verify(store).add(anyList());
    }

    @Test
    void vectorStoreFailureDoesNotBreakTheAssistant() {
        when(store.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("sin cuota"));
        when(chain.nextCall(any())).thenReturn(mock(ChatClientResponse.class));
        ChatClientRequest original = request("hola");

        advisor("key").adviseCall(original, chain);

        verify(chain).nextCall(original);
    }

    @Test
    void withoutApiKeyTheVectorStoreIsNotUsed() {
        when(chain.nextCall(any())).thenReturn(mock(ChatClientResponse.class));

        advisor("").adviseCall(request("hola"), chain);

        verifyNoInteractions(store);
    }

    @SuppressWarnings("unchecked")
    @Test
    void withoutEmbeddingsTheKnowledgeBaseFallsBackToKeywords() {
        ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
        KnowledgeBase knowledge = new KnowledgeBase(provider, "");

        List<String> found = knowledge.search("Puedo cancelar mi sesion con menos de 24 horas?");

        assertThat(found).isNotEmpty();
        assertThat(found.get(0)).startsWith("Cancelacion de sesiones");
    }
}
