package com.crmapi.sistemacrm.model;

import com.crmapi.sistemacrm.model.enums.StatusConversa;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "conversas")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendedor_id")
    private Usuario vendedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusConversa status;

    @Column(name = "nao_lidas", nullable = false)
    @Builder.Default
    private Integer naoLidas = 0;

    @Column(name = "ultima_mensagem_em")
    private LocalDateTime ultimaMensagemEm;

    @Column(name = "janela_expira_em")
    private LocalDateTime janelaExpiraEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "conversa", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Mensagem> mensagens = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "conversa", fetch = FetchType.LAZY)
    @Builder.Default
    private List<AtribuicaoLog> atribuicoesLog = new ArrayList<>();

    @PrePersist
    protected void aoPersistir() {
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
        if (this.status == null) {
            this.status = StatusConversa.ABERTA;
        }
        if (this.naoLidas == null) {
            this.naoLidas = 0;
        }
    }

    @PreUpdate
    protected void aoAtualizar() {
        this.atualizadoEm = LocalDateTime.now();
    }
}