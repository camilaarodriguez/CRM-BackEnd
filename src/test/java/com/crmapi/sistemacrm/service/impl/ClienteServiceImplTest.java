package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.cliente.ClienteCreateDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteReatribuirDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteResponseDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteStatusFunilDTO;
import com.crmapi.sistemacrm.dto.cliente.ClienteUpdateDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.mapper.ClienteMapper;
import com.crmapi.sistemacrm.model.AtribuicaoLog;
import com.crmapi.sistemacrm.model.Cliente;
import com.crmapi.sistemacrm.model.Conversa;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.StatusConversa;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import com.crmapi.sistemacrm.repository.AtribuicaoLogRepository;
import com.crmapi.sistemacrm.repository.ClienteRepository;
import com.crmapi.sistemacrm.repository.ConversaRepository;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import com.crmapi.sistemacrm.service.TelegramService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras da carteira de clientes, com todas as dependencias mockadas: repositorios,
 * o aviso ao Telegram e o registro de atribuicao. Nenhum teste abre transacao nem
 * sobe contexto Spring, entao a classe roda em milissegundos.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteServiceImpl: carteira de clientes")
class ClienteServiceImplTest {

    private static final String TELEFONE = "(11) 98877-6543";
    private static final String DOCUMENTO = "412.559.870-11";

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ConversaRepository conversaRepository;

    @Mock
    private AtribuicaoLogRepository atribuicaoLogRepository;

    @Spy
    private ClienteMapper clienteMapper = new ClienteMapper();

    @Mock
    private TelegramService telegramService;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    private final Usuario vendedor = Usuario.builder().id(3L).nome("Carlos").build();
    private final Usuario novoVendedor = Usuario.builder().id(4L).nome("Beatriz").build();

    private final ClienteUpdateDTO atualizacao = new ClienteUpdateDTO("Mariana Alves", "mariana@tech.com",
            TELEFONE, DOCUMENTO, "Tech Ltda", "Cliente VIP", 3L);

    private final ClienteReatribuirDTO reatribuicao = new ClienteReatribuirDTO(4L, "Ferias do vendedor");

    private ClienteCreateDTO novoCliente(StatusFunil status, String documento) {
        return new ClienteCreateDTO("Mariana Souza", "mariana@tech.com", TELEFONE,
                documento, "Tech Ltda", null, 3L, status);
    }

    private Cliente cliente(Long id, boolean ativo) {
        return Cliente.builder()
                .id(id)
                .nome("Mariana Souza")
                .telefone(TELEFONE)
                .vendedor(vendedor)
                .statusFunil(StatusFunil.NOVO)
                .ativo(ativo)
                .build();
    }

    private void salvarClienteComId(Long id) {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(id);
            return c;
        });
    }

    @Nested
    @DisplayName("Cadastro")
    class Criar {

        @Test
        @DisplayName("entra no funil como NOVO, abre conversa, registra a atribuicao e avisa a equipe")
        void deveCriarClienteComStatusNovoQuandoNaoInformado() {
            // Arrange
            ClienteCreateDTO dto = novoCliente(null, DOCUMENTO);
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(vendedor));
            salvarClienteComId(10L);
            when(conversaRepository.findByClienteId(10L)).thenReturn(Optional.of(new Conversa()));

            // Act
            ClienteResponseDTO resposta = clienteService.criar(dto);

            // Assert
            ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
            verify(clienteRepository).save(captor.capture());
            assertThat(captor.getValue().getStatusFunil()).isEqualTo(StatusFunil.NOVO);
            assertThat(resposta.vendedorId()).isEqualTo(3L);
            verify(conversaRepository).save(any(Conversa.class));
            verify(atribuicaoLogRepository).save(any(AtribuicaoLog.class));
            verify(telegramService).notificar(anyString());
        }

        @Test
        @DisplayName("respeita a etapa informada e nao confere documento quando ele nao veio")
        void deveCriarClienteComStatusInformadoESemDocumento() {
            // Arrange
            ClienteCreateDTO dto = novoCliente(StatusFunil.EM_CONTATO, null);
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(vendedor));
            salvarClienteComId(11L);
            when(conversaRepository.findByClienteId(11L)).thenReturn(Optional.of(new Conversa()));

            // Act
            ClienteResponseDTO resposta = clienteService.criar(dto);

            // Assert: o documento e opcional, entao a checagem de duplicidade nao roda.
            assertThat(resposta.statusFunil()).isEqualTo(StatusFunil.EM_CONTATO);
            verify(clienteRepository, never()).existsByDocumento(any());
        }

        @Test
        @DisplayName("recusa telefone que ja pertence a outro cliente")
        void naoDeveCriarClienteComTelefoneDuplicado() {
            // Arrange
            ClienteCreateDTO dto = novoCliente(StatusFunil.NOVO, DOCUMENTO);
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(vendedor));
            when(clienteRepository.existsByTelefone(TELEFONE)).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> clienteService.criar(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("telefone");
            verify(clienteRepository, never()).save(any());
        }

        @Test
        @DisplayName("recusa documento que ja pertence a outro cliente")
        void naoDeveCriarClienteComDocumentoDuplicado() {
            // Arrange
            ClienteCreateDTO dto = novoCliente(StatusFunil.NOVO, DOCUMENTO);
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(vendedor));
            when(clienteRepository.existsByTelefone(TELEFONE)).thenReturn(false);
            when(clienteRepository.existsByDocumento(DOCUMENTO)).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> clienteService.criar(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("documento");
        }

        @Test
        @DisplayName("recusa vendedor inexistente")
        void naoDeveCriarClienteComVendedorInexistente() {
            // Arrange
            ClienteCreateDTO dto = novoCliente(null, DOCUMENTO);
            when(usuarioRepository.findById(3L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> clienteService.criar(dto))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("converte falha do banco em erro de negocio")
        void deveLancarErroDeNegocioQuandoFalhaAoSalvarNaCriacao() {
            // Arrange
            ClienteCreateDTO dto = novoCliente(null, "  ");
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(vendedor));
            when(clienteRepository.save(any(Cliente.class))).thenThrow(new RuntimeException("banco fora"));

            // Act + Assert
            assertThatThrownBy(() -> clienteService.criar(dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel criar o cliente");
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultar {

        @Test
        @DisplayName("lista aplicando os filtros recebidos")
        void deveListarClientes() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 20);
            when(clienteRepository.findAll(ArgumentMatchers.<Specification<Cliente>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(cliente(10L, true))));

            // Act
            Page<ClienteResponseDTO> pagina = clienteService.listar("mari", StatusFunil.NOVO, 3L, false, pageable);

            // Assert
            assertThat(pagina.getContent()).extracting(ClienteResponseDTO::id).containsExactly(10L);
        }

        @Test
        @DisplayName("devolve o cliente procurado")
        void deveBuscarClientePorId() {
            // Arrange
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente(10L, true)));

            // Act
            ClienteResponseDTO resposta = clienteService.buscarPorId(10L);

            // Assert
            assertThat(resposta.nome()).isEqualTo("Mariana Souza");
        }

        @Test
        @DisplayName("lanca nao encontrado citando o id procurado")
        void deveLancarErroQuandoClienteNaoEncontrado() {
            // Arrange
            when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> clienteService.buscarPorId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("Edicao")
    class Atualizar {

        @Test
        @DisplayName("grava os novos dados cadastrais")
        void deveAtualizarCliente() {
            // Arrange
            Cliente existente = cliente(10L, true);
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(vendedor));
            when(clienteRepository.existsByTelefoneAndIdNot(TELEFONE, 10L)).thenReturn(false);
            when(clienteRepository.existsByDocumentoAndIdNot(DOCUMENTO, 10L)).thenReturn(false);
            when(clienteRepository.save(existente)).thenReturn(existente);

            // Act
            ClienteResponseDTO resposta = clienteService.atualizar(10L, atualizacao);

            // Assert
            assertThat(resposta.nome()).isEqualTo("Mariana Alves");
            assertThat(resposta.observacoes()).isEqualTo("Cliente VIP");
        }

        @Test
        @DisplayName("recusa telefone que ja pertence a outro cliente")
        void naoDeveAtualizarComTelefoneDeOutroCliente() {
            // Arrange
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente(10L, true)));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(vendedor));
            when(clienteRepository.existsByTelefoneAndIdNot(TELEFONE, 10L)).thenReturn(true);

            // Act + Assert: a checagem ignora o proprio registro, entao salvar sem
            // trocar o telefone continua valendo.
            assertThatThrownBy(() -> clienteService.atualizar(10L, atualizacao))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("telefone");
        }

        @Test
        @DisplayName("converte falha do banco em erro de negocio")
        void deveLancarErroDeNegocioQuandoFalhaAoSalvarNaAtualizacao() {
            // Arrange
            Cliente existente = cliente(10L, true);
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.findById(3L)).thenReturn(Optional.of(vendedor));
            when(clienteRepository.existsByTelefoneAndIdNot(TELEFONE, 10L)).thenReturn(false);
            when(clienteRepository.existsByDocumentoAndIdNot(DOCUMENTO, 10L)).thenReturn(false);
            when(clienteRepository.save(existente)).thenThrow(new RuntimeException("banco fora"));

            // Act + Assert
            assertThatThrownBy(() -> clienteService.atualizar(10L, atualizacao))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel atualizar o cliente");
        }
    }

    @Nested
    @DisplayName("Funil de vendas")
    class StatusDoFunil {

        @Test
        @DisplayName("move o cliente para a etapa pedida")
        void deveAtualizarStatusDoFunil() {
            // Arrange
            Cliente existente = cliente(10L, true);
            ClienteStatusFunilDTO dto = new ClienteStatusFunilDTO(StatusFunil.NEGOCIACAO);
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(existente));
            when(clienteRepository.save(existente)).thenReturn(existente);

            // Act
            ClienteResponseDTO resposta = clienteService.atualizarStatusFunil(10L, dto);

            // Assert
            assertThat(resposta.statusFunil()).isEqualTo(StatusFunil.NEGOCIACAO);
        }

        @Test
        @DisplayName("converte falha do banco em erro de negocio")
        void deveLancarErroDeNegocioQuandoFalhaAoAtualizarStatusDoFunil() {
            // Arrange
            Cliente existente = cliente(10L, true);
            ClienteStatusFunilDTO dto = new ClienteStatusFunilDTO(StatusFunil.PERDIDO);
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(existente));
            when(clienteRepository.save(existente)).thenThrow(new RuntimeException("banco fora"));

            // Act + Assert
            assertThatThrownBy(() -> clienteService.atualizarStatusFunil(10L, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel atualizar o status do funil");
        }
    }

    @Nested
    @DisplayName("Distribuicao entre vendedores")
    class Reatribuir {

        @Test
        @DisplayName("troca o responsavel, leva a conversa junto e registra de quem para quem")
        void deveReatribuirClienteComConversaExistente() {
            // Arrange
            Cliente existente = cliente(10L, true);
            Conversa conversa = Conversa.builder().id(20L).cliente(existente).vendedor(vendedor).build();
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.findById(4L)).thenReturn(Optional.of(novoVendedor));
            when(clienteRepository.save(existente)).thenReturn(existente);
            when(conversaRepository.findByClienteId(10L)).thenReturn(Optional.of(conversa));

            // Act
            ClienteResponseDTO resposta = clienteService.reatribuir(10L, reatribuicao);

            // Assert: sem levar a conversa junto, o cliente mudaria de dono mas o
            // atendimento continuaria na fila do vendedor antigo.
            assertThat(resposta.vendedorId()).isEqualTo(4L);
            assertThat(conversa.getVendedor()).isEqualTo(novoVendedor);
            ArgumentCaptor<AtribuicaoLog> captor = ArgumentCaptor.forClass(AtribuicaoLog.class);
            verify(atribuicaoLogRepository).save(captor.capture());
            assertThat(captor.getValue().getDeUsuario()).isEqualTo(vendedor);
            assertThat(captor.getValue().getMotivo()).isEqualTo("Ferias do vendedor");
            verify(telegramService).notificar(anyString());
        }

        @Test
        @DisplayName("abre uma conversa quando o cliente ainda nao tinha nenhuma")
        void deveCriarConversaAoReatribuirClienteSemConversaESemVendedor() {
            // Arrange
            Cliente semVendedor = cliente(10L, true);
            semVendedor.setVendedor(null);
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(semVendedor));
            when(usuarioRepository.findById(4L)).thenReturn(Optional.of(novoVendedor));
            when(clienteRepository.save(semVendedor)).thenReturn(semVendedor);
            when(conversaRepository.findByClienteId(10L)).thenReturn(Optional.empty());
            when(conversaRepository.save(any(Conversa.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            clienteService.reatribuir(10L, reatribuicao);

            // Assert
            ArgumentCaptor<Conversa> captor = ArgumentCaptor.forClass(Conversa.class);
            verify(conversaRepository, times(2)).save(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo(StatusConversa.ABERTA);
            assertThat(captor.getValue().getVendedor()).isEqualTo(novoVendedor);
        }

        @Test
        @DisplayName("converte falha do banco em erro de negocio")
        void deveLancarErroDeNegocioQuandoFalhaAoReatribuir() {
            // Arrange
            Cliente existente = cliente(10L, true);
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.findById(4L)).thenReturn(Optional.of(novoVendedor));
            when(clienteRepository.save(existente)).thenThrow(new RuntimeException("banco fora"));

            // Act + Assert
            assertThatThrownBy(() -> clienteService.reatribuir(10L, reatribuicao))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel reatribuir o cliente");
        }
    }

    @Nested
    @DisplayName("Exclusao")
    class Deletar {

        @Test
        @DisplayName("inativa o cliente em vez de apagar o registro")
        void deveInativarClienteAoDeletar() {
            // Arrange
            Cliente existente = cliente(10L, true);
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(existente));

            // Act
            clienteService.deletar(10L);

            // Assert: apagar de verdade quebraria as conversas e o historico de
            // atribuicoes que apontam para este cliente.
            assertThat(existente.getAtivo()).isFalse();
            verify(clienteRepository).save(existente);
        }

        @Test
        @DisplayName("recusa excluir quem ja esta inativo")
        void naoDeveDeletarClienteJaInativo() {
            // Arrange
            when(clienteRepository.findById(10L)).thenReturn(Optional.of(cliente(10L, false)));

            // Act + Assert
            assertThatThrownBy(() -> clienteService.deletar(10L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("inativo");
        }
    }
}
