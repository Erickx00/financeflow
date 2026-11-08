CREATE TABLE contas (
    id             UUID           NOT NULL,
    usuario_id     UUID           NOT NULL,
    nome           VARCHAR(100)   NOT NULL,
    tipo           VARCHAR(20)    NOT NULL,
    saldo_inicial  NUMERIC(19, 2) NOT NULL,
    moeda          VARCHAR(3)     NOT NULL DEFAULT 'BRL',
    ativa          BOOLEAN        NOT NULL DEFAULT TRUE,
    criado_em      TIMESTAMPTZ    NOT NULL,
    atualizado_em  TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pk_contas PRIMARY KEY (id),
    CONSTRAINT ck_contas_tipo CHECK (tipo IN ('CASH', 'CHECKING', 'SAVINGS')),
    CONSTRAINT ck_contas_moeda CHECK (moeda = 'BRL'),
    CONSTRAINT fk_contas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    -- Base da FK composta usada por transacoes: garante que a conta pertence ao dono.
    CONSTRAINT uk_contas_id_usuario UNIQUE (id, usuario_id)
);

CREATE INDEX ix_contas_usuario ON contas (usuario_id);
