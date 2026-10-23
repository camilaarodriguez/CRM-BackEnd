package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.mensagem.MensagemCreateDTO;
import com.crmapi.sistemacrm.dto.mensagem.MensagemResponseDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.mapper.MensagemMapper;
import com.crmapi.sistemacrm.model.Conversa;
import com.crmapi.sistemacrm.model.Mensagem;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.DirecaoMensagem;
import com.crmapi.sistemacrm.model.enums.StatusConversa;
import com.crmapi.sistemacrm.model.enums.StatusEntrega;
import com.crmapi.sistemacrm.model.enums.TipoMensagem;
import com.crmapi.sistemacrm.repository.ConversaRepository;
import com.crmapi.sistemacrm.repository.MensagemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras do envio e da leitura de mensagens, com repositorios mockados.
 *
 * Duas delas sao faceis de quebrar sem ninguem perceber pela tela: a conversa
 * precisa sair de ABERTA para EM_ATENDIMENTO no primeiro envio, e o contador de
 * nao lidas precisa zerar quando o historico e aberto.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MensagemServiceImpl: envio e leitura de mensagens")
class MensagemServiceImplTest {

    @Mock
    private MensagemRepository mensagemRepository;

    @Mock
    private ConversaRepository conversaRepository;

    @Spy
    private MensagemMapper mensagemMapper = new MensagemMapper();

    @InjectMocks
    private MensagemServiceImpl mensagemService;

    private final Usuario vendedor = Usuario.builder().id(3L).nome("Carlos").build();
    private final MensagemCreateDTO envio = new MensagemCreateDTO(20L, TipoMensagem.TEXTO, "Ola, Mariana!");

    private Conversa conversa(StatusConversa status, Integer naoLidas) {
        return Conversa.builder().id(20L).vendedor(vendedor).status(status).naoLidas(naoLidas).build();
    }

    @Nested
    @DisplayName("Envio")
    class Enviar {

        @Test
        @DisplayName("registra a mensagem como saida entregue e poe a conversa em atendimento")
        void deveEnviarMensagemEColocarConversaEmAtendimento() {
            // Arrange
            Conversa conversa = conversa(StatusConversa.ABERTA, 0);
            when(conversaRepository.findById(20L)).thenReturn(Optional.of(conversa));
            when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            MensagemResponseDTO resposta = mensagemService.enviar(envio);

            // Assert
            assertThat(resposta.direcao()).isEqualTo(DirecaoMensagem.SAIDA);
            assertThat(resposta.statusEntrega()).isEqualTo(StatusEntrega.ENVIADA);
            assertThat(resposta.enviadaPorId()).isEqualTo(3L);
            assertThat(conversa.getStatus()).isEqualTo(StatusConversa.EM_ATENDIMENTO);
            assertThat(conversa.getUltimaMensagemEm()).isNotNull();
            verify(conversaRepository).save(conversa);
        }

        @Test
        @DisplayName("nao reabre uma conversa que ja foi fechada")
        void deveManterStatusQuandoConversaNaoEstaAberta() {
            // Arrange
            Conversa conversa = conversa(StatusConversa.FECHADA, 0);
            when(conversaRepository.findById(20L)).thenReturn(Optional.of(conversa));
            when(mensagemRepository.save(any(Mensagem.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            mensagemService.enviar(envio);

            // Assert: so a transicao ABERTA -> EM_ATENDIMENTO e automatica.
            assertThat(conversa.getStatus()).isEqualTo(StatusConversa.FECHADA);
        }

        @Test
        @DisplayName("recusa o envio para uma conversa que nao existe")
        void naoDeveEnviarMensagemParaConversaInexistente() {
            // Arrange
            when(conversaRepository.findById(20L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> mensagemService.enviar(envio))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("20");
        }

        @Test
        @DisplayName("converte falha ao gravar em erro de negocio")
        void deveLancarErroDeNegocioQuandoFalhaAoSalvarMensagem() {
            // Arrange
            when(conversaRepository.findById(20L)).thenReturn(Optional.of(conversa(StatusConversa.ABERTA, 0)));
            when(mensagemRepository.save(any(Mensagem.class))).thenThrow(new RuntimeException("banco fora"));

            // Act + Assert
            assertThatThrownBy(() -> mensagemService.enviar(envio))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel enviar a mensagem");
        }
    }

    @Nested
    @DisplayName("Leitura do historico")
    class ListarPorConversa {

        @Test
        @DisplayName("devolve as mensagens e zera o contador de nao lidas")
        void deveListarMensagensEMarcarConversaComoLida() {
            // Arrange
            Conversa conversa = conversa(StatusConversa.ABERTA, 3);
            Mensagem mensagem = Mensagem.builder().id(1L).conversa(conversa).conteudo("Oi").build();
            when(conversaRepository.findById(20L)).thenReturn(Optional.of(conversa));
            when(mensagemRepository.findByConversaIdOrderByCriadoEmAsc(20L)).thenReturn(List.of(mensagem));

            // Act
            List<MensagemResponseDTO> mensagens = mensagemService.listarPorConversa(20L);

            // Assert: abrir o atendimento e o que marca como lido; sem isto o aviso de
            // nao lidas ficaria aceso para sempre.
            assertThat(mensagens).extracting(MensagemResponseDTO::conteudo).containsExactly("Oi");
            assertThat(conversa.getNaoLidas()).isZero();
            verify(conversaRepository).save(conversa);
        }

        @Test
        @DisplayName("nao grava a conversa quando ja estava sem nao lidas")
        void naoDeveSalvarConversaQuandoNaoHaMensagensNaoLidas() {
            // Arrange
            when(conversaRepository.findById(20L)).thenReturn(Optional.of(conversa(StatusConversa.ABERTA, 0)));
            when(mensagemRepository.findByConversaIdOrderByCriadoEmAsc(20L)).thenReturn(List.of());

            // Act
            List<MensagemResponseDTO> mensagens = mensagemService.listarPorConversa(20L);

            // Assert: evita uma escrita inutil a cada abertura de conversa.
            assertThat(mensagens).isEmpty();
            verify(conversaRepository, never()).save(any());
        }

        @Test
        @DisplayName("aguenta o contador nulo vindo de registro antigo")
        void naoDeveSalvarConversaQuandoContadorDeNaoLidasEstaNulo() {
            // Arrange
            when(conversaRepository.findById(20L)).thenReturn(Optional.of(conversa(StatusConversa.ABERTA, null)));
            when(mensagemRepository.findByConversaIdOrderByCriadoEmAsc(20L)).thenReturn(List.of());

            // Act
            mensagemService.listarPorConversa(20L);

            // Assert
            verify(conversaRepository, never()).save(any());
        }

        @Test
        @DisplayName("recusa a leitura de uma conversa que nao existe")
        void naoDeveListarMensagensDeConversaInexistente() {
            // Arrange
            when(conversaRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> mensagemService.listarPorConversa(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
