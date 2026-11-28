package com.healthdev.fisiovital.agent.memory;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Linea final "[opciones: a | b]" con la que se guardan en el historial las opciones que ofrecio el asistente. */
public final class ChoicesLine {

    private static final Pattern LINE = Pattern.compile("\\n\\n\\[opciones: (.+)]\\s*$");

    private ChoicesLine() {
    }

    static String format(List<String> choices) {
        return "\n\n[opciones: " + String.join(" | ", choices) + "]";
    }

    /** Texto sin la linea de opciones y las opciones que traia (vacia si no tenia). */
    public static Parsed parse(String content) {
        Matcher m = LINE.matcher(content);
        if (!m.find()) {
            return new Parsed(content, List.of());
        }
        List<String> choices = Arrays.stream(m.group(1).split("\\|")).map(String::strip)
                .filter(c -> !c.isEmpty()).toList();
        return new Parsed(content.substring(0, m.start()), choices);
    }

    public record Parsed(String text, List<String> choices) {
    }
}
