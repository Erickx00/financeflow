CREATE TABLE metas (
    id            UUID           NOT NULL,
    usuario_id    UUID           NOT NULL,
    nome          VARCHAR(120)   NOT NULL,
    valor_alvo    NUMERIC(19, 2) NOT NULL,
    valor_atual   NUMERIC(19, 2) NOT NULL DEFAULT 0,
    prazo         DATE,
    status        VARCHAR(20)    NOT NULL,
    versao        BIGINT         NOT NULL DEFAULT 0,
    criado_em     TIMESTAMPTZ    NOT NULL,
    atualizado_em TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_metas PRIMARY KEY (id),
    CONSTRAINT ck_metas_status CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT ck_metas_valor_alvo CHECK (valor_alvo > 0),
    CONSTRAINT ck_metas_valor_atual CHECK (valor_atual >= 0),
    CONSTRAINT fk_metas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX ix_metas_usuario_status ON metas (usuario_id, status);
