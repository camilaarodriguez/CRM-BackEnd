package com.crmapi.sistemacrm.model;

import com.crmapi.sistemacrm.model.enums.StatusConversa;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Callbacks @PrePersist e @PreUpdate das entidades, exercitados diretamente.
 *
 * Sao eles que garantem as colunas nao nulas de data e os valores padrao; os
 * testes de repositorio confirmam o mesmo efeito passando pelo JPA de verdade,
 * e aqui cada ramo e coberto sem o custo de subir contexto.
 */
@DisplayName("Callbacks de ciclo de vida das entidades")
class EntidadesCallbackTest {

    @Nested
    @DisplayName("Usuario")
    class DeUsuario {

        @Test
        @DisplayName("nasce ativo e com as datas preenchidas")
        void usuarioDevePreencherDatasEAtivarPorPadrao() {
            // Arrange
            Usuario usuario = Usuario.builder().ativo(null).build();

            // Act
            usuario.aoPersistir();

            // Assert
            assertThat(usuario.getAtivo()).isTrue();
            assertThat(usuario.getCriadoEm()).isNotNull();
            assertThat(usuario.getAtualizadoEm()).isNotNull();
        }

        @Test
        @DisplayName("respeita o status informado e atualiza a data na edicao")
        void usuarioDeveManterStatusInformadoEAtualizarData() {
            // Arrange
            Usuario usuario = Usuario.builder().ativo(false).build();

            // Act
            usuario.aoPersistir();
            usuario.aoAtualizar();

            // Assert
            assertThat(usuario.getAtivo()).isFalse();
            assertThat(usuario.getAtualizadoEm()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Cliente")
    class DeCliente {

        @Test
        @DisplayName("nasce ativo e na etapa NOVO do funil")
        void clienteDeveAplicarValoresPadrao() {
            // Arrange
            Cliente cliente = Cliente.builder().ativo(null).statusFunil(null).build();

            // Act
            cliente.aoPersistir();

            // Assert
            assertThat(cliente.getAtivo()).isTrue();
            assertThat(cliente.getStatusFunil()).isEqualTo(StatusFunil.NOVO);
            assertThat(cliente.getCriadoEm()).isNotNull();
        }

        @Test
        @DisplayName("respeita etapa e status informados e atualiza a data na edicao")
        void clienteDeveManterValoresInformadosEAtualizarData() {
            // Arrange
            Cliente cliente = Cliente.builder().ativo(false).statusFunil(StatusFunil.FECHADO).build();

            // Act
            cliente.aoPersistir();
            cliente.aoAtualizar();

            // Assert
            assertThat(cliente.getAtivo()).isFalse();
            assertThat(cliente.getStatusFunil()).isEqualTo(StatusFunil.FECHADO);
            assertThat(cliente.getAtualizadoEm()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Conversa")
    class DeConversa {

        @Test
        @DisplayName("nasce ABERTA e sem mensagens nao lidas")
        void conversaDeveAplicarValoresPadrao() {
            // Arrange
            Conversa conversa = Conversa.builder().status(null).naoLidas(null).build();

            // Act
            conversa.aoPersistir();

            // Assert
            assertThat(conversa.getStatus()).isEqualTo(StatusConversa.ABERTA);
            assertThat(conversa.getNaoLidas()).isZero();
            assertThat(conversa.getCriadoEm()).isNotNull();
        }

        @Test
        @DisplayName("respeita status e contador informados e atualiza a data na edicao")
        void conversaDeveManterValoresInformadosEAtualizarData() {
            // Arrange
            Conversa conversa = Conversa.builder().status(StatusConversa.FECHADA).naoLidas(5).build();

            // Act
            conversa.aoPersistir();
            conversa.aoAtualizar();

            // Assert
            assertThat(conversa.getStatus()).isEqualTo(StatusConversa.FECHADA);
            assertThat(conversa.getNaoLidas()).isEqualTo(5);
            assertThat(conversa.getAtualizadoEm()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Registros que so nascem")
    class SomenteCriacao {

        @Test
        @DisplayName("mensagem recebe a data de criacao")
        void mensagemDevePreencherDataDeCriacao() {
            // Arrange
            Mensagem mensagem = new Mensagem();

            // Act
            mensagem.aoPersistir();

            // Assert
            assertThat(mensagem.getCriadoEm()).isNotNull();
        }

        @Test
        @DisplayName("log de atribuicao recebe a data de criacao")
        void atribuicaoLogDevePreencherDataDeCriacao() {
            // Arrange
            AtribuicaoLog log = new AtribuicaoLog();

            // Act
            log.aoPersistir();

            // Assert: o historico de quem passou o cliente para quem precisa de data.
            assertThat(log.getCriadoEm()).isNotNull();
        }
    }
}
