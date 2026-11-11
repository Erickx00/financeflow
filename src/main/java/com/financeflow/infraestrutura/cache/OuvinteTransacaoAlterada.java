package com.financeflow.infraestrutura.cache;

import com.financeflow.transacao.EventoTransacaoAlterada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Invalida o cache somente depois do commit: se a transacao sofrer rollback,
 * o listener nunca roda e o cache anterior continua valido (ADR 0003).
 */
@Component
public class OuvinteTransacaoAlterada {

    private static final Logger log = LoggerFactory.getLogger(OuvinteTransacaoAlterada.class);

    private final ArmazemVersaoCache versions;

    public OuvinteTransacaoAlterada(ArmazemVersaoCache versions) {
        this.versions = versions;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionChanged(EventoTransacaoAlterada event) {
        try {
            versions.increment(event.usuarioId());
        } catch (RuntimeException ex) {
            // Nunca derruba a requisicao que ja foi commitada; o TTL cobre a perda.
            log.warn("Falha ao invalidar o cache do usuario {}", event.usuarioId(), ex);
        }
    }
}
