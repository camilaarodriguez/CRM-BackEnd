package com.crmapi.sistemacrm.repository;

import com.crmapi.sistemacrm.model.Cliente;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.repository.specification.ClienteSpecification;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fatia de persistencia do ClienteRepository.
 *
 * Alem das consultas derivadas, aqui as Specifications rodam contra um banco de
 * verdade: SpecificationsTest confere como o predicado e montado, e este confere
 * o que ele efetivamente filtra.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ClienteRepository")
class ClienteRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ClienteRepository clienteRepository;

    private Usuario ana;
    private Usuario carlos;

    @BeforeEach
    void prepararCarteira() {
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

    private Cliente cliente(String nome, String email, String telefone, String documento,
                            Usuario vendedor, StatusFunil status, boolean ativo) {
        return em.persist(Cliente.builder()
                .nome(nome)
                .email(email)
                .telefone(telefone)
                .documento(documento)
                .vendedor(vendedor)
                .statusFunil(status)
                .ativo(ativo)
                .build());
    }

    private Cliente mariana() {
        return cliente("Mariana Alves Souza", "mariana.souza@tech.com", "(11) 98877-6543",
                "412.559.870-11", ana, StatusFunil.NOVO, true);
    }

    @Nested
    @DisplayName("Checagem de duplicidade")
    class Duplicidade {

        @Test
        @DisplayName("existsByTelefone acusa um telefone ja usado")
        void deveAcusarTelefoneExistente() {
            // Arrange
            mariana();
            em.flush();

            // Act + Assert
            assertThat(clienteRepository.existsByTelefone("(11) 98877-6543")).isTrue();
            assertThat(clienteRepository.existsByTelefone("(11) 90000-0000")).isFalse();
        }

        @Test
        @DisplayName("existsByTelefoneAndIdNot ignora o proprio registro")
        void deveIgnorarOProprioRegistroAoConferirOTelefone() {
            // Arrange
            Cliente gravado = mariana();
            em.flush();

            // Act + Assert: sem este "AndIdNot", salvar o cliente sem trocar o telefone
            // acusaria duplicidade contra ele mesmo e travaria toda edicao.
            assertThat(clienteRepository.existsByTelefoneAndIdNot("(11) 98877-6543", gravado.getId())).isFalse();
            assertThat(clienteRepository.existsByTelefoneAndIdNot("(11) 98877-6543", gravado.getId() + 1)).isTrue();
        }

        @Test
        @DisplayName("existsByDocumento acusa um documento ja usado")
        void deveAcusarDocumentoExistente() {
            // Arrange
            mariana();
            em.flush();

            // Act + Assert
            assertThat(clienteRepository.existsByDocumento("412.559.870-11")).isTrue();
            assertThat(clienteRepository.existsByDocumento("000.000.000-00")).isFalse();
        }

        @Test
        @DisplayName("existsByDocumentoAndIdNot ignora o proprio registro")
        void deveIgnorarOProprioRegistroAoConferirODocumento() {
            // Arrange
            Cliente gravado = mariana();
            em.flush();

            // Act + Assert
            assertThat(clienteRepository.existsByDocumentoAndIdNot("412.559.870-11", gravado.getId())).isFalse();
            assertThat(clienteRepository.existsByDocumentoAndIdNot("412.559.870-11", gravado.getId() + 1)).isTrue();
        }
    }

    @Nested
    @DisplayName("ClienteSpecification executada no banco")
    class Filtros {

        @BeforeEach
        void popularCarteira() {
            cliente("Mariana Alves Souza", "mariana.souza@tech.com", "(11) 98877-6543",
                    "412.559.870-11", ana, StatusFunil.NOVO, true);
            cliente("Rafael Nogueira Lima", "rafael.lima@vetorlog.com", "(21) 99654-1200",
                    "22.145.908/0001-73", carlos, StatusFunil.NEGOCIACAO, true);
            cliente("Juliana Prado", "juliana.prado@casamineira.com.br", "(31) 98432-7788",
                    "18.720.455/0001-09", ana, StatusFunil.NEGOCIACAO, true);
            cliente("Eduardo Tavares", "eduardo.ramos@sul.com.br", "(41) 99123-4567",
                    "09.882.140/0001-55", ana, StatusFunil.FECHADO, false);
            em.flush();
            em.clear();
        }

        private List<String> nomesFiltrados(String busca, StatusFunil status, Long vendedorId, Boolean inativos) {
            Page<Cliente> pagina = clienteRepository.findAll(
                    ClienteSpecification.comFiltros(busca, status, vendedorId, inativos),
                    PageRequest.of(0, 20));
            return pagina.getContent().stream().map(Cliente::getNome).toList();
        }

        @Test
        @DisplayName("sem filtro nenhum traz apenas os clientes ativos")
        void deveEsconderOsInativosPorPadrao() {
            // Act
            List<String> nomes = nomesFiltrados(null, null, null, null);

            // Assert: Eduardo foi excluido por soft delete e nao pode aparecer na carteira.
            assertThat(nomes).hasSize(3).doesNotContain("Eduardo Tavares");
        }

        @Test
        @DisplayName("com incluirInativos traz tambem os excluidos")
        void deveTrazerOsInativosQuandoPedido() {
            // Act
            List<String> nomes = nomesFiltrados(null, null, null, true);

            // Assert
            assertThat(nomes).hasSize(4).contains("Eduardo Tavares");
        }

        @Test
        @DisplayName("a busca encontra por parte do nome, ignorando a caixa")
        void deveBuscarPorParteDoNome() {
            // Act
            List<String> nomes = nomesFiltrados("MARIANA", null, null, null);

            // Assert
            assertThat(nomes).containsExactly("Mariana Alves Souza");
        }

        @Test
        @DisplayName("a busca tambem olha o e-mail")
        void deveBuscarPeloEmail() {
            // Act
            List<String> nomes = nomesFiltrados("vetorlog", null, null, null);

            // Assert
            assertThat(nomes).containsExactly("Rafael Nogueira Lima");
        }

        @Test
        @DisplayName("filtra pela etapa do funil")
        void deveFiltrarPelaEtapaDoFunil() {
            // Act
            List<String> nomes = nomesFiltrados(null, StatusFunil.NEGOCIACAO, null, null);

            // Assert
            assertThat(nomes).containsExactlyInAnyOrder("Rafael Nogueira Lima", "Juliana Prado");
        }

        @Test
        @DisplayName("filtra pela carteira de um vendedor")
        void deveFiltrarPeloVendedor() {
            // Act
            List<String> nomes = nomesFiltrados(null, null, carlos.getId(), null);

            // Assert
            assertThat(nomes).containsExactly("Rafael Nogueira Lima");
        }

        @Test
        @DisplayName("combina busca, etapa e vendedor na mesma consulta")
        void deveCombinarTodosOsFiltros() {
            // Act
            List<String> nomes = nomesFiltrados("prado", StatusFunil.NEGOCIACAO, ana.getId(), null);

            // Assert
            assertThat(nomes).containsExactly("Juliana Prado");
        }

        @Test
        @DisplayName("devolve pagina vazia quando os filtros nao casam entre si")
        void deveDevolverVazioQuandoOsFiltrosNaoCasam() {
            // Act: Juliana esta em NEGOCIACAO, mas na carteira da Ana, nao do Carlos.
            List<String> nomes = nomesFiltrados("prado", StatusFunil.NEGOCIACAO, carlos.getId(), null);

            // Assert
            assertThat(nomes).isEmpty();
        }
    }
}
