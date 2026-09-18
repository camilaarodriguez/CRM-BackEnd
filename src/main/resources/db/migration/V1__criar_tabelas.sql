CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    senha_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP
);

CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL,
    telefone VARCHAR(20) NOT NULL UNIQUE,
    documento VARCHAR(18) UNIQUE,
    empresa VARCHAR(150),
    observacoes TEXT,
    vendedor_id BIGINT,
    status_funil VARCHAR(20) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP,
    CONSTRAINT fk_clientes_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios(id)
);

CREATE TABLE conversas (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT NOT NULL,
    vendedor_id BIGINT,
    status VARCHAR(20) NOT NULL,
    nao_lidas INT NOT NULL DEFAULT 0,
    ultima_mensagem_em TIMESTAMP,
    janela_expira_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP,
    CONSTRAINT fk_conversas_cliente FOREIGN KEY (cliente_id) REFERENCES clientes(id),
    CONSTRAINT fk_conversas_vendedor FOREIGN KEY (vendedor_id) REFERENCES usuarios(id)
);

CREATE TABLE mensagens (
    id BIGSERIAL PRIMARY KEY,
    conversa_id BIGINT NOT NULL,
    direcao VARCHAR(10) NOT NULL,
    tipo VARCHAR(15) NOT NULL,
    conteudo TEXT NOT NULL,
    wa_message_id VARCHAR(80) UNIQUE,
    status_entrega VARCHAR(15),
    enviada_por_id BIGINT,
    criado_em TIMESTAMP NOT NULL,
    CONSTRAINT fk_mensagens_conversa FOREIGN KEY (conversa_id) REFERENCES conversas(id),
    CONSTRAINT fk_mensagens_usuario FOREIGN KEY (enviada_por_id) REFERENCES usuarios(id)
);

CREATE TABLE atribuicoes_log (
    id BIGSERIAL PRIMARY KEY,
    conversa_id BIGINT NOT NULL,
    de_usuario_id BIGINT,
    para_usuario_id BIGINT NOT NULL,
    feita_por_id BIGINT NOT NULL,
    motivo VARCHAR(255),
    criado_em TIMESTAMP NOT NULL,
    CONSTRAINT fk_log_conversa FOREIGN KEY (conversa_id) REFERENCES conversas(id),
    CONSTRAINT fk_log_de_usuario FOREIGN KEY (de_usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_log_para_usuario FOREIGN KEY (para_usuario_id) REFERENCES usuarios(id),
    CONSTRAINT fk_log_feita_por FOREIGN KEY (feita_por_id) REFERENCES usuarios(id)
);