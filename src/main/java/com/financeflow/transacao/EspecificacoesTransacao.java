package com.financeflow.transacao;

import com.financeflow.transacao.dto.FiltroTransacao;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

/** Filtros combinaveis; o filtro por proprietario e sempre o primeiro. */
public final class EspecificacoesTransacao {

    private EspecificacoesTransacao() {
    }

    public static Specification<Transacao> ownedBy(UUID usuarioId) {
        return (root, query, cb) -> cb.equal(root.get("usuarioId"), usuarioId);
    }

    public static Specification<Transacao> matching(FiltroTransacao filtro) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (filtro.tipo() != null) {
                predicates.add(cb.equal(root.get("tipo"), filtro.tipo()));
            }
            if (filtro.status() != null) {
                predicates.add(cb.equal(root.get("status"), filtro.status()));
            }
            if (filtro.contaId() != null) {
                predicates.add(cb.equal(root.get("contaId"), filtro.contaId()));
            }
            if (filtro.categoriaId() != null) {
                predicates.add(cb.equal(root.get("categoriaId"), filtro.categoriaId()));
            }
            LocalDate inicio = filtro.dataInicio();
            LocalDate fim = filtro.dataFim();
            if (inicio != null && fim != null) {
                predicates.add(cb.between(root.get("dataTransacao"), inicio, fim));
            } else if (inicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataTransacao"), inicio));
            } else if (fim != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataTransacao"), fim));
            }
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
