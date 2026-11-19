package com.healthdev.fisiovital.agent.mcp;

import com.healthdev.fisiovital.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** MCP: el servidor publica solo herramientas de lectura y el endpoint /mcp pide JWT. */
class McpServerTest extends IntegrationTestBase {

    private static final String INITIALIZE = """
            {"jsonrpc":"2.0","id":1,"method":"initialize","params":{"protocolVersion":"2025-06-18",
            "capabilities":{},"clientInfo":{"name":"test","version":"1.0"}}}""";

    @Autowired
    private List<ToolCallbackProvider> providers;

    @Autowired
    private FisioVitalMcpTools tools;

    @Test
    void publishesOnlyReadOnlyTools() {
        List<String> names = providers.stream()
                .flatMap(p -> Arrays.stream(p.getToolCallbacks()))
                .map(ToolCallback::getToolDefinition)
                .map(d -> d.name())
                .toList();

        assertThat(names).contains("listSpecialties", "findPhysiotherapists", "getPolicies")
                .doesNotContain("bookSession", "rescheduleSession");
    }

    @Test
    void findPhysiotherapistsReturnsActivePhysiosOfTheSpecialty() throws Exception {
        newPhysio("NEUROLOGICA");

        List<FisioVitalMcpTools.PhysioInfo> found = tools.findPhysiotherapists("neurologica");

        assertThat(found).isNotEmpty().allMatch(p -> p.specialty().equals("NEUROLOGICA"));
        assertThat(tools.findPhysiotherapists("no-existe")).isEmpty();
    }

    @Test
    void getPoliciesSearchesTheKnowledgeBase() {
        assertThat(tools.getPolicies("cancelar con menos de 24 horas").get(0)).startsWith("Cancelacion de sesiones");
        assertThat(tools.listSpecialties()).contains("COLUMNA_POSTURA", "DEPORTIVA");
    }

    @Test
    void mcpEndpointRequiresJwt() throws Exception {
        mockMvc.perform(post("/mcp").contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content(INITIALIZE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void mcpEndpointInitializesWithJwt() throws Exception {
        mockMvc.perform(authed(post("/mcp"), newPatientToken()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                        .content(INITIALIZE))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("fisiovital-mcp")));
    }
}
