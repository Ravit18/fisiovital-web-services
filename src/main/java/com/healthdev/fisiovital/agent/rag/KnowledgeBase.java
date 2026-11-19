package com.healthdev.fisiovital.agent.rag;

import com.healthdev.fisiovital.agent.advisor.KnowledgeAdvisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Base de conocimiento de FisioVital (knowledge/fisiovital-faq.txt, fragmentos separados por "---").
 * Busqueda semantica con Embeddings + VectorStore; la usan el KnowledgeAdvisor (RAG del chat) y el
 * servidor MCP. Si no hay embeddings activos, el MCP cae a una busqueda por palabras.
 */
@Slf4j
@Component
public class KnowledgeBase {

    static final int TOP_K = 3;
    private static final String KNOWLEDGE_FILE = "knowledge/fisiovital-faq.txt";
    private static final Duration RETRY_LOAD_AFTER = Duration.ofMinutes(5);
    private static final int MIN_WORD_LENGTH = 4;

    private final VectorStore vectorStore;
    private final boolean configured;
    private final List<String> fragments;
    private boolean loaded;
    private long lastAttempt;

    public KnowledgeBase(ObjectProvider<VectorStore> vectorStore, @Value("${app.ai.gemini.api-key:}") String apiKey) {
        this.vectorStore = vectorStore.getIfAvailable();
        this.configured = !apiKey.isBlank();
        this.fragments = readFragments();
    }

    /** Busqueda semantica (RAG). Vacia si no hay embeddings o si falla: el asistente sigue sin contexto. */
    public List<String> semanticSearch(String question) {
        if (vectorStore == null || !configured) {
            return List.of();
        }
        try {
            ensureLoaded();
            return loaded ? vectorStore.similaritySearch(SearchRequest.builder().query(question).topK(TOP_K).build())
                    .stream().map(Document::getText).toList() : List.of();
        } catch (Exception e) {
            log.warn("RAG no disponible, se responde sin contexto: {}", e.getMessage());
            return List.of();
        }
    }

    /** Semantica si se puede; si no, los fragmentos que comparten mas palabras con la pregunta. */
    public List<String> search(String question) {
        List<String> semantic = semanticSearch(question);
        return semantic.isEmpty() ? keywordSearch(question) : semantic;
    }

    List<String> keywordSearch(String question) {
        Set<String> words = words(question);
        return fragments.stream()
                .map(f -> new Scored(f, words(f).stream().filter(words::contains).count()))
                .filter(s -> s.score() > 0)
                .sorted(Comparator.comparingLong(Scored::score).reversed())
                .limit(TOP_K)
                .map(Scored::text)
                .toList();
    }

    private record Scored(String text, long score) {
    }

    private static Set<String> words(String text) {
        String plain = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(plain.split("[^a-z0-9]+"))
                .filter(w -> w.length() >= MIN_WORD_LENGTH)
                .collect(Collectors.toSet());
    }

    private synchronized void ensureLoaded() {
        long now = System.currentTimeMillis();
        if (loaded || now - lastAttempt < RETRY_LOAD_AFTER.toMillis()) {
            return;
        }
        lastAttempt = now;
        vectorStore.add(fragments.stream().map(Document::new).toList());
        loaded = true;
    }

    private static List<String> readFragments() {
        try {
            String text = new ClassPathResource(KNOWLEDGE_FILE).getContentAsString(StandardCharsets.UTF_8);
            return Arrays.stream(text.split("(?m)^---\\s*$")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + KNOWLEDGE_FILE, e);
        }
    }
}
