package com.crmapi.sistemacrm.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "atribuicoes_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtribuicaoLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversa_id", nullable = false)
    private Conversa conversa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "de_usuario_id")
    private Usuario deUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "para_usuario_id", nullable = false)
    private Usuario paraUsuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feita_por_id", nullable = false)
    private Usuario feitaPor;

    @Column(length = 255)
    private String motivo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoPersistir() {
        this.criadoEm = LocalDateTime.now();
    }
}