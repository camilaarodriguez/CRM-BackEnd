package com.crmapi.sistemacrm.repository;

import com.crmapi.sistemacrm.model.Cliente;
import com.crmapi.sistemacrm.model.Conversa;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.StatusConversa;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fatia de persistencia do ConversaRepository: a fila de atendimento por vendedor
 * e a conversa unica de cada cliente.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ConversaRepository")
class ConversaRepositoryTest {

    private final AtomicInteger sequencia = new AtomicInteger(1);

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ConversaRepository conversaRepository;

    private Usuario ana;
    private Usuario carlos;

    @BeforeEach
    void prepararEquipe() {
        ana = vendedor("Ana Beatriz Moraes", "ana.moraes@crm.com");
        carlos = vendedor("Carlos Eduardo Lima", "carlos.lima@crm.com");
        em.flush();
    }

    private Usuario vendedor(String nome, String email) {
        return em.persist(Usuario.builder()
                .nome(nome)
                .email(email)
                .senha("$2a$10$hashBCryptDeExemplo")
                .role(UsuarioRole.VENDEDOR)
                .build());
    }

    private Cliente cliente(String nome) {
        int n = sequencia.getAndIncrement();
        return em.persist(Cliente.builder()
                .nome(nome)
                .email("cliente" + n + "@empresa.com")
                .telefone("(11) 9000-000" + n)
                .statusFunil(StatusFunil.NOVO)
                .build());
    }

    private Conversa conversa(Cliente cliente, Usuario vendedor) {
        return em.persist(Conversa.builder()
                .cliente(cliente)
                .vendedor(vendedor)
                .status(StatusConversa.ABERTA)
                .build());
    }

    @Nested
    @DisplayName("findByVendedorId")
    class FilaDoVendedor {

        @Test
        @DisplayName("traz apenas as conversas do vendedor pedido")
        void deveTrazerApenasAsConversasDoVendedor() {
            // Arrange
            conversa(cliente("Mariana"), ana);
            conversa(cliente("Juliana"), ana);
            conversa(cliente("Rafael"), carlos);
            em.flush();
            em.clear();

            // Act
            Page<Conversa> fila = conversaRepository.findByVendedorId(ana.getId(), PageRequest.of(0, 20));

            // Assert
            assertThat(fila.getTotalElements()).isEqualTo(2);
            assertThat(fila.getContent())
                    .extracting(c -> c.getCliente().getNome())
                    .containsExactlyInAnyOrder("Mariana", "Juliana");
        }

        @Test
        @DisplayName("devolve fila vazia para um vendedor sem atendimentos")
        void deveDevolverFilaVazia() {
            // Arrange
            conversa(cliente("Mariana"), ana);
            em.flush();
            em.clear();

            // Act
            Page<Conversa> fila = conversaRepository.findByVendedorId(carlos.getId(), PageRequest.of(0, 20));

            // Assert
            assertThat(fila).isEmpty();
        }

        @Test
        @DisplayName("quebra o resultado em paginas do tamanho pedido")
        void deveRespeitarAPaginacao() {
            // Arrange
            conversa(cliente("Mariana"), ana);
            conversa(cliente("Juliana"), ana);
            conversa(cliente("Camila"), ana);
            em.flush();
            em.clear();

            // Act
            Page<Conversa> primeira = conversaRepository.findByVendedorId(ana.getId(), PageRequest.of(0, 2));
            Page<Conversa> segunda = conversaRepository.findByVendedorId(ana.getId(), PageRequest.of(1, 2));

            // Assert
            assertThat(primeira.getContent()).hasSize(2);
            assertThat(segunda.getContent()).hasSize(1);
            assertThat(primeira.getTotalPages()).isEqualTo(2);
            assertThat(primeira.getTotalElements()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("findByClienteId")
    class ConversaDoCliente {

        @Test
        @DisplayName("devolve a conversa aberta para aquele cliente")
        void deveEncontrarAConversaDoCliente() {
            // Arrange
            Cliente mariana = cliente("Mariana");
            Conversa gravada = conversa(mariana, ana);
            em.flush();
            em.clear();

            // Act
            Optional<Conversa> encontrada = conversaRepository.findByClienteId(mariana.getId());

            // Assert
            assertThat(encontrada).isPresent();
            assertThat(encontrada.get().getId()).isEqualTo(gravada.getId());
            assertThat(encontrada.get().getStatus()).isEqualTo(StatusConversa.ABERTA);
        }

        @Test
        @DisplayName("devolve vazio para cliente que ainda nao tem conversa")
        void deveDevolverVazioSemConversa() {
            // Arrange
            Cliente semConversa = cliente("Rafael");
            em.flush();
            em.clear();

            // Act
            Optional<Conversa> encontrada = conversaRepository.findByClienteId(semConversa.getId());

            // Assert
            assertThat(encontrada).isEmpty();
        }
    }

    @Nested
    @DisplayName("Callbacks da tabela")
    class Mapeamento {

        @Test
        @DisplayName("uma conversa nova nasce ABERTA e sem nao lidas")
        void devePreencherOsValoresPadrao() {
            // Act
            Conversa gravada = em.persistAndFlush(Conversa.builder()
                    .cliente(cliente("Mariana"))
                    .vendedor(ana)
                    .build());

            // Assert
            assertThat(gravada.getStatus()).isEqualTo(StatusConversa.ABERTA);
            assertThat(gravada.getNaoLidas()).isZero();
            assertThat(gravada.getCriadoEm()).isNotNull();
        }
    }
}
