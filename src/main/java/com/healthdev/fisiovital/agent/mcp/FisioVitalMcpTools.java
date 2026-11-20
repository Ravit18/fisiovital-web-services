package com.healthdev.fisiovital.agent.mcp;

import com.healthdev.fisiovital.model.Specialty;
import com.healthdev.fisiovital.agent.rag.KnowledgeBase;
import com.healthdev.fisiovital.service.PhysiotherapistSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Herramientas que FisioVital publica por MCP (Model Context Protocol) para clientes externos
 * (MCP Inspector u otro agente compatible con MCP). Son de SOLO LECTURA: ninguna reserva ni modifica datos,
 * eso queda para el asistente interno, que verifica el rol del paciente.
 */
@Component
@RequiredArgsConstructor
public class FisioVitalMcpTools {

    static final int MAX_PHYSIOS = 5;

    private final PhysiotherapistSearchService physiotherapistSearchService;
    private final KnowledgeBase knowledgeBase;

    public record PhysioInfo(String fullName, String specialty, int yearsOfExperience, boolean hasAvailability) {
    }

    @Tool(name = "listSpecialties", description = "Lista las especialidades de fisioterapia que atiende FisioVital.")
    public List<String> listSpecialties() {
        return Arrays.stream(Specialty.values()).map(Enum::name).toList();
    }

    @Tool(name = "findPhysiotherapists", description = "Busca fisioterapeutas activos de FisioVital de una "
            + "especialidad, ordenados por anios de experiencia. Usa listSpecialties para ver los valores validos.")
    public List<PhysioInfo> findPhysiotherapists(
            @ToolParam(description = "Especialidad, por ejemplo COLUMNA_POSTURA o DEPORTIVA") String specialty) {
        Specialty parsed;
        try {
            parsed = Specialty.valueOf(specialty.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return List.of();
        }
        return physiotherapistSearchService.search(parsed).items().stream()
                .limit(MAX_PHYSIOS)
                .map(p -> new PhysioInfo(p.fullName(), p.specialty(), p.yearsOfExperience(), p.hasAvailability()))
                .toList();
    }

    @Tool(name = "getPolicies", description = "Responde dudas sobre las politicas de FisioVital (cancelacion, "
            + "reprogramacion, planes, recordatorios, historia clinica, cuenta) buscando en su base de conocimiento.")
    public List<String> getPolicies(@ToolParam(description = "Pregunta o tema, por ejemplo 'cancelar con menos "
            + "de 24 horas'") String question) {
        return knowledgeBase.search(question);
    }
}
