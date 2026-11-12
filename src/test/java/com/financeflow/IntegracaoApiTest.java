package com.financeflow;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao completo com o contexto web real, banco H2 em modo
 * PostgreSQL e Redis configurado para uma porta sem servico. Prova, no mesmo
 * fluxo, autenticacao, isolamento por usuario, regras financeiras e o fallback
 * do cache (ADR 0003).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("it")
class IntegracaoApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("fluxo completo: cadastro, login, conta, categoria, transacao, saldo e relatorio")
    void completeFinancialFlow() throws Exception {
        String token = registerAndLogin("fluxo-" + UUID.randomUUID() + "@example.com");

        String accountJson = mockMvc.perform(post("/api/v1/contas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Conta principal","tipo":"CHECKING","saldoInicial":1000.00,"moeda":"BRL"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ativa").value(true))
                .andReturn().getResponse().getContentAsString();
        String contaId = JsonPath.read(accountJson, "$.id");

        String categoryJson = mockMvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Mercado","tipo":"EXPENSE","cor":"#FF8800"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String categoriaId = JsonPath.read(categoryJson, "$.id");

        mockMvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contaId":"%s","categoriaId":"%s","descricao":"Compra no supermercado",
                                 "valor":250.90,"tipo":"EXPENSE","dataTransacao":"2026-11-13","status":"COMPLETED"}
                                """.formatted(contaId, categoriaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/contas/{id}/saldo", contaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(749.10));

        mockMvc.perform(get("/api/v1/relatorios/resumo")
                        .param("mes", "2026-11")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("2026-11"))
                .andExpect(jsonPath("$.totalExpenses").value(250.90))
                .andExpect(jsonPath("$.saldo").value(-250.90));

        mockMvc.perform(get("/api/v1/relatorios/categorias")
                        .param("mes", "2026-11")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias[0].nome").value("Mercado"))
                .andExpect(jsonPath("$.categorias[0].percentage").value(100.00));

        // Categoria em uso nunca e apagada: 409 para o cliente decidir.
        mockMvc.perform(delete("/api/v1/categorias/{id}", categoriaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        // Transacao pendente nao entra no relatorio ate ser concluida.
        mockMvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contaId":"%s","categoriaId":"%s","descricao":"Pendente",
                                 "valor":50.00,"tipo":"EXPENSE","dataTransacao":"2026-11-20","status":"PENDING"}
                                """.formatted(contaId, categoriaId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/relatorios/resumo")
                        .param("mes", "2026-11")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.totalExpenses").value(250.90));
    }

    @Test
    @DisplayName("requisicao sem token retorna 401 no contrato de erro")
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/contas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("usuario nao acessa recurso de outro usuario e recebe 404")
    void cannotAccessAnotherUsersResource() throws Exception {
        String ownerToken = registerAndLogin("owner-" + UUID.randomUUID() + "@example.com");
        String accountJson = mockMvc.perform(post("/api/v1/contas")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Privada","tipo":"CASH","saldoInicial":10.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String contaId = JsonPath.read(accountJson, "$.id");

        String intruderToken = registerAndLogin("intruder-" + UUID.randomUUID() + "@example.com");
        mockMvc.perform(get("/api/v1/contas/{id}", contaId)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("relatorios continuam funcionando com o Redis indisponivel")
    void reportsWorkWithoutRedis() throws Exception {
        String token = registerAndLogin("redis-" + UUID.randomUUID() + "@example.com");

        for (int attempt = 1; attempt <= 2; attempt++) {
            MvcResult result = mockMvc.perform(get("/api/v1/relatorios/resumo")
                            .param("mes", "2026-11")
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            assertThat(result.getResponse().getContentAsString()).contains("\"period\":\"2026-11\"");
        }
    }

    @Test
    @DisplayName("payload invalido retorna 400 com erros de campo")
    void invalidPayloadReturnsBadRequest() throws Exception {
        String token = registerAndLogin("invalid-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/v1/contas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"","tipo":"CHECKING","saldoInicial":-1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("transacao com categoria de tipo incompativel e rejeitada")
    void rejectsIncompatibleCategory() throws Exception {
        String token = registerAndLogin("mismatch-" + UUID.randomUUID() + "@example.com");
        String contaId = createAccount(token);
        String incomeCategory = createCategory(token, "Salario", "INCOME");

        mockMvc.perform(post("/api/v1/transacoes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contaId":"%s","categoriaId":"%s","descricao":"Compra",
                                 "valor":10.00,"tipo":"EXPENSE","dataTransacao":"2026-11-13","status":"PENDING"}
                                """.formatted(contaId, incomeCategory)))
                .andExpect(status().isBadRequest());
    }

    private String createAccount(String token) throws Exception {
        String json = mockMvc.perform(post("/api/v1/contas")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Conta","tipo":"CHECKING","saldoInicial":0.00}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String createCategory(String token, String nome, String tipo) throws Exception {
        String json = mockMvc.perform(post("/api/v1/categorias")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"%s","tipo":"%s"}
                                """.formatted(nome, tipo)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.id");
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/autenticacao/cadastrar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Erick","email":"%s","senha":"SenhaMuitoForte123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        String loginJson = mockMvc.perform(post("/api/v1/autenticacao/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","senha":"SenhaMuitoForte123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(loginJson, "$.accessToken");
    }
}
