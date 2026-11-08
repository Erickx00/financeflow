CREATE TABLE transacoes (
    id              UUID           NOT NULL,
    usuario_id      UUID           NOT NULL,
    conta_id        UUID           NOT NULL,
    categoria_id    UUID           NOT NULL,
    descricao       VARCHAR(200)   NOT NULL,
    valor           NUMERIC(19, 2) NOT NULL,
    tipo            VARCHAR(20)    NOT NULL,
    data_transacao  DATE           NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    versao          BIGINT         NOT NULL DEFAULT 0,
    criado_em       TIMESTAMPTZ    NOT NULL,
    atualizado_em   TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_transacoes PRIMARY KEY (id),
    CONSTRAINT ck_transacoes_tipo CHECK (tipo IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_transacoes_status CHECK (status IN ('PENDING', 'COMPLETED')),
    CONSTRAINT ck_transacoes_valor CHECK (valor > 0),
    CONSTRAINT fk_transacoes_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    -- O banco garante que conta e categoria pertencem ao mesmo dono da transacao.
    CONSTRAINT fk_transacoes_conta FOREIGN KEY (conta_id, usuario_id)
        REFERENCES contas (id, usuario_id) ON DELETE RESTRICT,
    CONSTRAINT fk_transacoes_categoria FOREIGN KEY (categoria_id, usuario_id)
        REFERENCES categorias (id, usuario_id) ON DELETE RESTRICT,
    -- E garante que o tipo da categoria e igual ao tipo da transacao.
    CONSTRAINT fk_transacoes_categoria_tipo FOREIGN KEY (categoria_id, tipo)
        REFERENCES categorias (id, tipo) ON DELETE RESTRICT
);

CREATE INDEX ix_transacoes_usuario_data ON transacoes (usuario_id, data_transacao);
CREATE INDEX ix_transacoes_conta_data ON transacoes (conta_id, data_transacao);
CREATE INDEX ix_transacoes_usuario_categoria ON transacoes (usuario_id, categoria_id);
-- Indice parcial: praticamente todo relatorio filtra por usuario + periodo + COMPLETED.
CREATE INDEX ix_transacoes_usuario_concluidas ON transacoes (usuario_id, data_transacao)
    WHERE status = 'COMPLETED';
