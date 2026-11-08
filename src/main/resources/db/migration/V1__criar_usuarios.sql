CREATE TABLE usuarios (
    id            UUID         NOT NULL,
    nome          VARCHAR(120) NOT NULL,
    email         VARCHAR(180) NOT NULL,
    senha_hash    VARCHAR(255) NOT NULL,
    papel         VARCHAR(20)  NOT NULL,
    criado_em     TIMESTAMPTZ  NOT NULL,
    atualizado_em TIMESTAMPTZ  NOT NULL,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT ck_usuarios_papel CHECK (papel IN ('USER', 'ADMIN'))
);

-- Unicidade sobre o e-mail normalizado: "A@x.com" e "a@x.com" sao o mesmo usuario.
CREATE UNIQUE INDEX uk_usuarios_email_normalizado ON usuarios (lower(email));
