package com.crmapi.sistemacrm.model;

import com.crmapi.sistemacrm.model.enums.DirecaoMensagem;
import com.crmapi.sistemacrm.model.enums.StatusEntrega;
import com.crmapi.sistemacrm.model.enums.TipoMensagem;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensagens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensagem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversa_id", nullable = false)
    private Conversa conversa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DirecaoMensagem direcao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoMensagem tipo;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String conteudo;

    @Column(name = "wa_message_id", unique = true, length = 80)
    private String waMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_entrega", length = 15)
    private StatusEntrega statusEntrega;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enviada_por_id")
    private Usuario enviadaPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoPersistir() {
        this.criadoEm = LocalDateTime.now();
    }
}