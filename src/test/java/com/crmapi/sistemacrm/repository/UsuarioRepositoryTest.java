package com.crmapi.sistemacrm.repository;

import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Fatia de persistencia do UsuarioRepository. Sobe so o JPA contra o H2 em memoria
 * configurado em src/test/resources/application.properties, sem servicos nem web.
 *
 * Os testes de servico mockam o repositorio; aqui as consultas derivadas e as
 * restricoes de coluna sao executadas de verdade, que e a unica forma de provar
 * que elas existem e fazem o que o nome promete.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("UsuarioRepository")
class UsuarioRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UsuarioRepository usuarioRepository;

    private Usuario gravar(String email) {
        Usuario usuario = Usuario.builder()
                .nome("Ana Beatriz Moraes")
                .email(email)
                .senha("$2a$10$hashBCryptDeExemplo")
                .role(UsuarioRole.VENDEDOR)
                .build();
        return em.persistAndFlush(usuario);
    }

    @BeforeEach
    void limpar() {
        em.clear();
    }

    @Nested
    @DisplayName("findByEmail")
    class BuscarPorEmail {

        @Test
        @DisplayName("devolve o usuario gravado com aquele e-mail")
        void deveEncontrarPeloEmail() {
            // Arrange
            Usuario gravado = gravar("ana.moraes@crm.com");

            // Act
            Optional<Usuario> encontrado = usuarioRepository.findByEmail("ana.moraes@crm.com");

            // Assert
            assertThat(encontrado).isPresent();
            assertThat(encontrado.get().getId()).isEqualTo(gravado.getId());
            assertThat(encontrado.get().getNome()).isEqualTo("Ana Beatriz Moraes");
        }

        @Test
        @DisplayName("devolve vazio quando ninguem usa aquele e-mail")
        void deveDevolverVazioParaEmailInexistente() {
            // Arrange
            gravar("ana.moraes@crm.com");

            // Act
            Optional<Usuario> encontrado = usuarioRepository.findByEmail("ninguem@crm.com");

            // Assert
            assertThat(encontrado).isEmpty();
        }

        @Test
        @DisplayName("distingue maiusculas de minusculas")
        void aBuscaPorEmailDistingueMaiusculasDeMinusculas() {
            // Arrange
            gravar("ana.moraes@crm.com");

            // Act
            Optional<Usuario> encontrado = usuarioRepository.findByEmail("Ana.Moraes@CRM.com");

            // Assert: comportamento atual da consulta derivada. Quem digitar o e-mail
            // com outra caixa no login nao sera encontrado.
            assertThat(encontrado).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByEmail")
    class ExisteEmail {

        @Test
        @DisplayName("acusa verdadeiro para e-mail ja cadastrado")
        void deveAcusarEmailExistente() {
            // Arrange
            gravar("ana.moraes@crm.com");

            // Act + Assert
            assertThat(usuarioRepository.existsByEmail("ana.moraes@crm.com")).isTrue();
        }

        @Test
        @DisplayName("acusa falso para e-mail livre")
        void deveAcusarEmailLivre() {
            // Arrange
            gravar("ana.moraes@crm.com");

            // Act + Assert
            assertThat(usuarioRepository.existsByEmail("livre@crm.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("Restricoes e callbacks da tabela")
    class Mapeamento {

        @Test
        @DisplayName("o banco recusa dois usuarios com o mesmo e-mail")
        void naoDevePermitirEmailDuplicado() {
            // Arrange
            gravar("ana.moraes@crm.com");

            // Act + Assert: a checagem no servico evita o caminho comum, mas a
            // restricao unique e a garantia final contra corrida entre requisicoes.
            assertThatThrownBy(() -> gravar("ana.moraes@crm.com"))
                    .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("preenche criadoEm, atualizadoEm e ativo ao gravar")
        void devePreencherOsCamposDeControle() {
            // Act
            Usuario gravado = gravar("ana.moraes@crm.com");

            // Assert
            assertThat(gravado.getId()).isNotNull();
            assertThat(gravado.getCriadoEm()).isNotNull();
            assertThat(gravado.getAtualizadoEm()).isNotNull();
            assertThat(gravado.getAtivo()).isTrue();
        }

        @Test
        @DisplayName("grava a senha na coluna senha_hash")
        void deveGravarASenhaNaColunaSenhaHash() {
            // Arrange
            Usuario gravado = gravar("ana.moraes@crm.com");

            // Act: consulta nativa para conferir o nome real da coluna, e nao o do campo.
            Object valor = em.getEntityManager()
                    .createNativeQuery("SELECT senha_hash FROM usuarios WHERE id = :id")
                    .setParameter("id", gravado.getId())
                    .getSingleResult();

            // Assert
            assertThat(valor).isEqualTo("$2a$10$hashBCryptDeExemplo");
        }
    }
}
