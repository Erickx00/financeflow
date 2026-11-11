package com.financeflow.transacao;

import java.util.UUID;

/**
 * Publicado dentro da transacao e consumido somente depois do commit, para que
 * uma alteracao que sofra rollback nao invalide o cache (ADR 0003).
 */
public record EventoTransacaoAlterada(UUID usuarioId) {
}
