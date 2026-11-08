CREATE TABLE categorias (
    id            UUID         NOT NULL,
    usuario_id    UUID         NOT NULL,
    nome          VARCHAR(80)  NOT NULL,
    tipo          VARCHAR(20)  NOT NULL,
    cor           VARCHAR(7),
    criado_em     TIMESTAMPTZ  NOT NULL,
    atualizado_em TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_categorias PRIMARY KEY (id),
    CONSTRAINT ck_categorias_tipo CHECK (tipo IN ('INCOME', 'EXPENSE')),
    CONSTRAINT ck_categorias_cor CHECK (cor IS NULL OR cor ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT fk_categorias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uk_categorias_id_usuario UNIQUE (id, usuario_id),
    -- Base da FK composta que impede transacao INCOME com categoria EXPENSE.
    CONSTRAINT uk_categorias_id_tipo UNIQUE (id, tipo),
    CONSTRAINT uk_categorias_usuario_nome_tipo UNIQUE (usuario_id, nome, tipo)
);

CREATE INDEX ix_categorias_usuario_tipo ON categorias (usuario_id, tipo);
