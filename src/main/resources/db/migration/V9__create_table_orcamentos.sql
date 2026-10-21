CREATE TABLE orcamentos
(
    id                    BIGSERIAL      NOT NULL,
    ordem_servico_id      BIGINT         NOT NULL,
    valor_mao_de_obra     NUMERIC(10, 2) NOT NULL,
    valor_pecas           NUMERIC(10, 2) NOT NULL,
    valor_total           NUMERIC(10, 2) NOT NULL,
    data_criacao          TIMESTAMP      NOT NULL,
    data_expiracao        TIMESTAMP      NOT NULL,
    status                VARCHAR(20)    NOT NULL,
    data_resposta         TIMESTAMP,
    justificativa_cliente TEXT,

    CONSTRAINT pk_orcamentos PRIMARY KEY (id),
    CONSTRAINT uk_orcamentos_os UNIQUE (ordem_servico_id),
    CONSTRAINT fk_orcamentos_os FOREIGN KEY (ordem_servico_id) REFERENCES ordens_servico (id)
);
