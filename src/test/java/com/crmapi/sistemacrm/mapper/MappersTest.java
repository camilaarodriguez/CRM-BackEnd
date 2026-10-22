package com.crmapi.sistemacrm.mapper;

import com.crmapi.sistemacrm.dto.cliente.ClienteResponseDTO;
import com.crmapi.sistemacrm.dto.conversa.ConversaResponseDTO;
import com.crmapi.sistemacrm.dto.mensagem.MensagemResponseDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioCreateDTO;
import com.crmapi.sistemacrm.model.Cliente;
import com.crmapi.sistemacrm.model.Conversa;
import com.crmapi.sistemacrm.model.Mensagem;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Conversao entre entidade e DTO. O risco destes mapeamentos esta nos vinculos
 * opcionais: cliente sem vendedor, conversa sem responsavel, mensagem recebida
 * do cliente e portanto sem autor interno. Em todos esses casos o mapper precisa
 * devolver nulo em vez de estourar NullPointerException.
 */
@DisplayName("Mapeadores de entidade para DTO")
class MappersTest {

    private final Usuario vendedor = Usuario.builder().id(3L).nome("Carlos").build();
    private final Cliente cliente = Cliente.builder().id(10L).nome("Mariana").vendedor(vendedor).build();

    @Nested
    @DisplayName("ClienteMapper")
    class DeCliente {

        @Test
        @DisplayName("leva o vendedor responsavel para o DTO")
        void deveMapearClienteComVendedor() {
            // Act
            ClienteResponseDTO dto = new ClienteMapper().paraResponseDTO(cliente);

            // Assert
            assertThat(dto.vendedorId()).isEqualTo(3L);
            assertThat(dto.vendedorNome()).isEqualTo("Carlos");
        }

        @Test
        @DisplayName("aceita cliente ainda sem vendedor")
        void deveMapearClienteSemVendedor() {
            // Act
            ClienteResponseDTO dto = new ClienteMapper().paraResponseDTO(Cliente.builder().id(11L).build());

            // Assert
            assertThat(dto.vendedorId()).isNull();
            assertThat(dto.vendedorNome()).isNull();
        }
    }

    @Nested
    @DisplayName("ConversaMapper")
    class DeConversa {

        @Test
        @DisplayName("leva cliente e vendedor para o DTO")
        void deveMapearConversaComClienteEVendedor() {
            // Arrange
            Conversa conversa = Conversa.builder().id(20L).cliente(cliente).vendedor(vendedor).build();

            // Act
            ConversaResponseDTO dto = new ConversaMapper().paraResponseDTO(conversa);

            // Assert
            assertThat(dto.clienteId()).isEqualTo(10L);
            assertThat(dto.clienteNome()).isEqualTo("Mariana");
            assertThat(dto.vendedorId()).isEqualTo(3L);
            assertThat(dto.vendedorNome()).isEqualTo("Carlos");
        }

        @Test
        @DisplayName("aceita conversa sem cliente nem responsavel")
        void deveMapearConversaSemClienteNemVendedor() {
            // Act
            ConversaResponseDTO dto = new ConversaMapper().paraResponseDTO(Conversa.builder().id(21L).build());

            // Assert
            assertThat(dto.clienteId()).isNull();
            assertThat(dto.vendedorNome()).isNull();
        }
    }

    @Nested
    @DisplayName("MensagemMapper")
    class DeMensagem {

        @Test
        @DisplayName("leva a conversa e o autor para o DTO")
        void deveMapearMensagemComConversaEAutor() {
            // Arrange
            Mensagem mensagem = Mensagem.builder()
                    .id(1L)
                    .conversa(Conversa.builder().id(20L).build())
                    .enviadaPor(vendedor)
                    .build();

            // Act
            MensagemResponseDTO dto = new MensagemMapper().paraResponseDTO(mensagem);

            // Assert
            assertThat(dto.conversaId()).isEqualTo(20L);
            assertThat(dto.enviadaPorId()).isEqualTo(3L);
            assertThat(dto.enviadaPorNome()).isEqualTo("Carlos");
        }

        @Test
        @DisplayName("aceita mensagem recebida, que nao tem autor interno")
        void deveMapearMensagemSemConversaNemAutor() {
            // Act
            MensagemResponseDTO dto = new MensagemMapper().paraResponseDTO(Mensagem.builder().id(2L).build());

            // Assert
            assertThat(dto.conversaId()).isNull();
            assertThat(dto.enviadaPorId()).isNull();
        }
    }

    @Nested
    @DisplayName("UsuarioMapper")
    class DeUsuario {

        @Test
        @DisplayName("monta o usuario ativo com a senha ja codificada")
        void deveMapearCriacaoDeUsuarioParaEntidadeAtiva() {
            // Arrange
            UsuarioCreateDTO dto = new UsuarioCreateDTO("Ana", "ana@crm.com", "123456", UsuarioRole.ADMIN);

            // Act
            Usuario usuario = new UsuarioMapper().paraEntidade(dto, "$2a$10$hashJaCodificado");

            // Assert: o mapper recebe o hash pronto e nao tem acesso ao texto puro,
            // entao nao ha caminho possivel para a senha original chegar ao banco.
            assertThat(usuario.getSenha()).isEqualTo("$2a$10$hashJaCodificado");
            assertThat(usuario.getSenha()).isNotEqualTo(dto.senha());
            assertThat(usuario.getRole()).isEqualTo(UsuarioRole.ADMIN);
            assertThat(usuario.getAtivo()).isTrue();
        }
    }
}
