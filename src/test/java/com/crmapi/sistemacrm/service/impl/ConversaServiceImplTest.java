package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.conversa.ConversaResponseDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.mapper.ConversaMapper;
import com.crmapi.sistemacrm.model.Conversa;
import com.crmapi.sistemacrm.model.enums.StatusConversa;
import com.crmapi.sistemacrm.repository.ConversaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras da fila de atendimento, com o repositorio mockado: nenhum teste aqui
 * encosta no banco. As consultas em si sao verificadas em ConversaRepositoryTest.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ConversaServiceImpl: fila de atendimento")
class ConversaServiceImplTest {

    @Mock
    private ConversaRepository conversaRepository;

    @Spy
    private ConversaMapper conversaMapper = new ConversaMapper();

    @InjectMocks
    private ConversaServiceImpl conversaService;

    private final Pageable pageable = PageRequest.of(0, 20);

    private Conversa conversa(Long id) {
        return Conversa.builder().id(id).status(StatusConversa.ABERTA).naoLidas(2).build();
    }

    @Nested
    @DisplayName("Listagem")
    class Listar {

        @Test
        @DisplayName("com vendedorId busca apenas a fila daquele vendedor")
        void deveListarConversasDoVendedor() {
            // Arrange
            when(conversaRepository.findByVendedorId(3L, pageable))
                    .thenReturn(new PageImpl<>(List.of(conversa(1L))));

            // Act
            Page<ConversaResponseDTO> pagina = conversaService.listar(3L, pageable);

            // Assert: o vendedor nao pode enxergar a operacao inteira, entao a consulta
            // ampla nem chega a ser chamada.
            assertThat(pagina.getContent()).extracting(ConversaResponseDTO::id).containsExactly(1L);
            verify(conversaRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("sem vendedorId devolve a operacao inteira")
        void deveListarTodasAsConversasQuandoSemVendedor() {
            // Arrange
            when(conversaRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(conversa(1L), conversa(2L))));

            // Act
            Page<ConversaResponseDTO> pagina = conversaService.listar(null, pageable);

            // Assert
            assertThat(pagina.getTotalElements()).isEqualTo(2);
        }

        @Test
        @DisplayName("converte falha do banco em erro de negocio, sem vazar a causa")
        void deveLancarErroDeNegocioQuandoFalhaAoListar() {
            // Arrange
            when(conversaRepository.findAll(pageable)).thenThrow(new RuntimeException("banco fora"));

            // Act + Assert
            assertThatThrownBy(() -> conversaService.listar(null, pageable))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel listar as conversas");
        }
    }

    @Nested
    @DisplayName("Busca por id")
    class BuscarPorId {

        @Test
        @DisplayName("devolve a conversa com status e contador de nao lidas")
        void deveBuscarConversaPorId() {
            // Arrange
            when(conversaRepository.findById(1L)).thenReturn(Optional.of(conversa(1L)));

            // Act
            ConversaResponseDTO resposta = conversaService.buscarPorId(1L);

            // Assert
            assertThat(resposta.status()).isEqualTo(StatusConversa.ABERTA);
            assertThat(resposta.naoLidas()).isEqualTo(2);
        }

        @Test
        @DisplayName("lanca nao encontrado citando o id procurado")
        void deveLancarNaoEncontradoQuandoConversaNaoExiste() {
            // Arrange
            when(conversaRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> conversaService.buscarPorId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("converte falha do banco em erro de negocio")
        void deveLancarErroDeNegocioQuandoFalhaAoBuscar() {
            // Arrange
            when(conversaRepository.findById(1L)).thenThrow(new RuntimeException("banco fora"));

            // Act + Assert
            assertThatThrownBy(() -> conversaService.buscarPorId(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel buscar a conversa");
        }
    }
}
