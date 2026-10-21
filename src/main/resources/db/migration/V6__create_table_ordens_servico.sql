CREATE TABLE ordens_servico
(
    id                BIGSERIAL   NOT NULL,
    numero_protocolo  VARCHAR(30) NOT NULL,
    cliente_id        BIGINT      NOT NULL,
    equipamento_id    BIGINT      NOT NULL,
    tecnico_id        BIGINT,
    status            VARCHAR(30) NOT NULL,
    descricao_problema TEXT       NOT NULL,
    data_abertura     TIMESTAMP   NOT NULL,
    data_finalizacao  TIMESTAMP,
    data_entrega      TIMESTAMP,

    CONSTRAINT pk_ordens_servico PRIMARY KEY (id),
    CONSTRAINT uk_ordens_servico_protocolo UNIQUE (numero_protocolo),
    CONSTRAINT fk_os_cliente FOREIGN KEY (cliente_id) REFERENCES clientes (id),
    CONSTRAINT fk_os_equipamento FOREIGN KEY (equipamento_id) REFERENCES equipamentos (id),
    CONSTRAINT fk_os_tecnico FOREIGN KEY (tecnico_id) REFERENCES tecnicos (id)
);
