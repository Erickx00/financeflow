package com.financeflow.goal;

import com.financeflow.comum.exception.ExcecaoRegraNegocio;
import com.financeflow.comum.exception.ExcecaoConflito;
import com.financeflow.comum.exception.ExcecaoRecursoNaoEncontrado;
import com.financeflow.goal.dto.RequisicaoCriarMeta;
import com.financeflow.goal.dto.RequisicaoAporteMeta;
import com.financeflow.goal.dto.RespostaMeta;
import com.financeflow.goal.dto.RequisicaoAtualizarMeta;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class MetaService {

    private final MetaRepository goals;

    public MetaService(MetaRepository goals) {
        this.goals = goals;
    }

    @Transactional
    public RespostaMeta criar(UUID usuarioId, RequisicaoCriarMeta request) {
        Meta meta = Meta.criar(usuarioId, request.nome().trim(), money(request.valorAlvo()), request.prazo());
        return MetaMapper.toResponse(goals.save(meta));
    }

    @Transactional(readOnly = true)
    public List<RespostaMeta> listar(UUID usuarioId, StatusMeta status) {
        List<Meta> found = status == null
                ? goals.findByUsuarioIdOrderByCriadoEmDesc(usuarioId)
                : goals.findByUsuarioIdAndStatusOrderByCriadoEmDesc(usuarioId, status);
        return found.stream().map(MetaMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public RespostaMeta get(UUID usuarioId, UUID metaId) {
        return MetaMapper.toResponse(carregar(usuarioId, metaId));
    }

    @Transactional
    public RespostaMeta atualizar(UUID usuarioId, UUID metaId, RequisicaoAtualizarMeta request) {
        Meta meta = carregar(usuarioId, metaId);
        meta.updateDetails(request.nome().trim(), money(request.valorAlvo()), request.prazo());
        return MetaMapper.toResponse(meta);
    }

    @Transactional
    public RespostaMeta contribuir(UUID usuarioId, UUID metaId, RequisicaoAporteMeta request) {
        Meta meta = carregar(usuarioId, metaId);
        if (meta.getStatus() != StatusMeta.ACTIVE) {
            throw new ExcecaoConflito("Somente metas ativas aceitam contribuicoes");
        }
        meta.contribuir(money(request.valor()));
        return MetaMapper.toResponse(meta);
    }

    @Transactional
    public RespostaMeta cancelar(UUID usuarioId, UUID metaId) {
        Meta meta = carregar(usuarioId, metaId);
        if (meta.getStatus() != StatusMeta.ACTIVE) {
            throw new ExcecaoConflito("Somente metas ativas podem ser canceladas");
        }
        meta.cancelar();
        return MetaMapper.toResponse(meta);
    }

    /**
     * Excluir apaga o historico de aportes: so e permitido quando nada foi aportado.
     */
    @Transactional
    public void excluir(UUID usuarioId, UUID metaId) {
        Meta meta = carregar(usuarioId, metaId);
        if (meta.getValorAtual().compareTo(BigDecimal.ZERO) > 0) {
            throw new ExcecaoConflito("Meta com aportes nao pode ser excluida; cancele-a");
        }
        goals.delete(meta);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private Meta carregar(UUID usuarioId, UUID metaId) {
        return goals.findByIdAndUsuarioId(metaId, usuarioId)
                .orElseThrow(() -> new ExcecaoRecursoNaoEncontrado("Meta"));
    }
}
