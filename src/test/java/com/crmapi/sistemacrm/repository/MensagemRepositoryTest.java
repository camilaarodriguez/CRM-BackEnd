package com.crmapi.sistemacrm.repository;

import com.crmapi.sistemacrm.model.Cliente;
import com.crmapi.sistemacrm.model.Conversa;
import com.crmapi.sistemacrm.model.Mensagem;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.DirecaoMensagem;
import com.crmapi.sistemacrm.model.enums.StatusConversa;
import com.crmapi.sistemacrm.model.enums.StatusEntrega;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import com.crmapi.sistemacrm.model.enums.TipoMensagem;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fatia de persistencia do MensagemRepository: o historico de um atendimento,
 * que precisa chegar a tela na ordem em que foi escrito.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("MensagemRepository")
class MensagemRepositoryTest {

    private final AtomicInteger sequencia = new AtomicInteger(1);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private MensagemRepository mensagemRepository;

    private Usuario carlos;
    private Conversa atendimento;
    private Conversa outroAtendimento;

    @BeforeEach
    void prepararAtendimento() {
        carlos = em.persist(Usuario.builder()
                .nome("Carlos Eduardo Lima")
                .email("carlos.lima@crm.com")
                .senha("$2a$10$hashBCryptDeExemplo")
                .role(UsuarioRole.VENDEDOR)
                .build());
        atendimento = conversa("Mariana");
        outroAtendimento = conversa("Rafael");
        em.flush();
    }

    private Conversa conversa(String nomeDoCliente) {
        int n = sequencia.getAndIncrement();
        Cliente cliente = em.persist(Cliente.builder()
                .nome(nomeDoCliente)
                .email("cliente" + n + "@empresa.com")
                .telefone("(11) 9000-000" + n)
                .statusFunil(StatusFunil.NOVO)
                .build());
        return em.persist(Conversa.builder()
                .cliente(cliente)
                .vendedor(carlos)
                .status(StatusConversa.ABERTA)
                .build());
    }

    /**
     * Grava a mensagem e depois crava a data por consulta nativa. O @PrePersist usa
     * LocalDateTime.now(), e tres insercoes seguidas poderiam cair no mesmo instante;
     * com datas explicitas o teste de ordenacao para de depender do relogio.
     */
    private Mensagem mensagem(Conversa conversa, String conteudo, LocalDateTime quando) {
        Mensagem gravada = em.persistAndFlush(Mensagem.builder()
                .conversa(conversa)
                .direcao(DirecaoMensagem.SAIDA)
                .tipo(TipoMensagem.TEXTO)
                .conteudo(conteudo)
                .statusEntrega(StatusEntrega.ENVIADA)
                .enviadaPor(carlos)
                .build());
        em.getEntityManager()
                .createNativeQuery("UPDATE mensagens SET criado_em = :quando WHERE id = :id")
                .setParameter("quando", quando)
                .setParameter("id", gravada.getId())
                .executeUpdate();
        return gravada;
    }

    @Test
    @DisplayName("devolve o historico da conversa em ordem cronologica")
    void deveDevolverOHistoricoEmOrdemCronologica() {
        // Arrange: gravadas fora de ordem de proposito.
        LocalDateTime base = LocalDateTime.of(2026, 3, 10, 9, 0);
        mensagem(atendimento, "Terceira", base.plusMinutes(20));
        mensagem(atendimento, "Primeira", base);
        mensagem(atendimento, "Segunda", base.plusMinutes(10));
        em.clear();

        // Act
        List<Mensagem> historico = mensagemRepository.findByConversaIdOrderByCriadoEmAsc(atendimento.getId());

        // Assert
        assertThat(historico)
                .extracting(Mensagem::getConteudo)
                .containsExactly("Primeira", "Segunda", "Terceira");
    }

    @Test
    @DisplayName("nao mistura mensagens de outra conversa")
    void naoDeveMisturarAsConversas() {
        // Arrange
        LocalDateTime base = LocalDateTime.of(2026, 3, 10, 9, 0);
        mensagem(atendimento, "Ola, Mariana!", base);
        mensagem(outroAtendimento, "Ola, Rafael!", base.plusMinutes(1));
        em.clear();

        // Act
        List<Mensagem> historico = mensagemRepository.findByConversaIdOrderByCriadoEmAsc(atendimento.getId());

        // Assert
        assertThat(historico).hasSize(1);
        assertThat(historico.get(0).getConteudo()).isEqualTo("Ola, Mariana!");
    }

    @Test
    @DisplayName("devolve lista vazia para conversa sem mensagens")
    void deveDevolverListaVaziaSemMensagens() {
        // Act
        List<Mensagem> historico = mensagemRepository.findByConversaIdOrderByCriadoEmAsc(outroAtendimento.getId());

        // Assert
        assertThat(historico).isEmpty();
    }

    @Test
    @DisplayName("guarda o conteudo longo que a coluna TEXT comporta")
    void deveGuardarConteudoLongo() {
        // Arrange: a coluna conteudo e TEXT justamente para nao truncar mensagem grande.
        String conteudoLongo = "x".repeat(5000);

        // Act
        Mensagem gravada = mensagem(atendimento, conteudoLongo, LocalDateTime.of(2026, 3, 10, 9, 0));
        em.clear();
        Mensagem relida = mensagemRepository.findById(gravada.getId()).orElseThrow();

        // Assert
        assertThat(relida.getConteudo()).hasSize(5000).isEqualTo(conteudoLongo);
    }

    @Test
    @DisplayName("preenche a data de criacao ao gravar")
    void devePreencherADataDeCriacao() {
        // Act
        Mensagem gravada = em.persistAndFlush(Mensagem.builder()
                .conversa(atendimento)
                .direcao(DirecaoMensagem.ENTRADA)
                .tipo(TipoMensagem.TEXTO)
                .conteudo("Bom dia!")
                .build());

        // Assert
        assertThat(gravada.getId()).isNotNull();
        assertThat(gravada.getCriadoEm()).isNotNull();
    }
}
