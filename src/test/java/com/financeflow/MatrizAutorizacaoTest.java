package com.financeflow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Matriz de autorizacao: todo recurso criado por um usuario responde 404 para
 * qualquer outro usuario, em leitura e em escrita (BOLA/IDOR).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
class MatrizAutorizacaoTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("nenhum endpoint permite ler, alterar ou excluir recurso de outro usuario")
    void everyResourceIsIsolatedByOwner() throws Exception {
        String owner = registerAndLogin();
        String intruder = registerAndLogin();

        String contaId = createAccount(owner);
        String categoriaId = createCategory(owner, "Mercado", "EXPENSE");
        String transacaoId = createTransaction(owner, contaId, categoriaId);
        String metaId = createGoal(owner);

        // Contas
        mockMvc.perform(get("/api/v1/contas/{id}", contaId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/contas/{id}/saldo", contaId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/contas/{id}", contaId)
                        .header("Authorization", bearer(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Invadida\",\"tipo\":\"CASH\"}"))
                .andExpect(status().isNotFound());

        // Categorias
        mockMvc.perform(get("/api/v1/categorias/{id}", categoriaId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/categorias/{id}", categoriaId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());

        // Transacoes
        mockMvc.perform(get("/api/v1/transacoes/{id}", transacaoId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/transacoes/{id}", transacaoId)
                        .header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());

        // Metas
        mockMvc.perform(get("/api/v1/metas/{id}", metaId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());

        // A lista do intruso nao mostra dados do dono
        mockMvc.perform(get("/api/v1/transacoes").header("Authorization", bearer(intruder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get("/api/v1/contas").header("Authorization", bearer(intruder)))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get("/api/v1/metas").header("Authorization", bearer(intruder)))
                .andExpect(jsonPath("$.length()").value(0));

        // O dono continua enxergando tudo
        mockMvc.perform(get("/api/v1/transacoes/{id}", transacaoId)
                        .header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(transacaoId));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String createAccount(String token) throws Exception {
        String json = mockMvc.perform(post("/api/v1/contas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Conta\",\"tipo\":\"CHECKING\",\"saldoInicial\":10.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String createCategory(String token, String nome, String tipo) throws Exception {
        String json = mockMvc.perform(post("/api/v1/categorias")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"%s\",\"tipo\":\"%s\"}".formatted(nome, tipo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String createTransaction(String token, String contaId, String categoriaId) throws Exception {
        String json = mockMvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contaId":"%s","categoriaId":"%s","descricao":"Compra","valor":5.00,
                                 "tipo":"EXPENSE","dataTransacao":"2026-11-13","status":"COMPLETED"}
                                """.formatted(contaId, categoriaId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String createGoal(String token) throws Exception {
        String json = mockMvc.perform(post("/api/v1/metas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Reserva\",\"valorAlvo\":1000.00}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String registerAndLogin() throws Exception {
        String email = "usuario-" + UUID.randomUUID() + "@example.com";
        mockMvc.perform(post("/api/v1/autenticacao/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Usuario\",\"email\":\"%s\",\"senha\":\"SenhaMuitoForte123\"}"
                                .formatted(email)))
                .andExpect(status().isCreated());

        String json = mockMvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"SenhaMuitoForte123\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.accessToken");
    }
}
