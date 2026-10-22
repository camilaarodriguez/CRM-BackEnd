package com.crmapi.sistemacrm.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Traducao de excecao para resposta HTTP. Cada caso de erro precisa chegar ao
 * cliente com o status certo e sem vazar detalhe interno.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/clientes/9");

    @Nested
    @DisplayName("Erros do cliente")
    class ErrosDoCliente {

        @Test
        @DisplayName("recurso inexistente vira 404 com o caminho pedido")
        void deveTratarRecursoNaoEncontrado() {
            // Act
            ResponseEntity<ErrorResponseDTO> resposta =
                    handler.tratarNaoEncontrado(new ResourceNotFoundException("Cliente nao encontrado"), request);

            // Assert
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(resposta.getBody()).isNotNull();
            assertThat(resposta.getBody().mensagem()).isEqualTo("Cliente nao encontrado");
            assertThat(resposta.getBody().caminho()).isEqualTo("/api/clientes/9");
        }

        @Test
        @DisplayName("erro de validacao vira 400 listando os campos")
        void deveTratarErroDeValidacaoListandoOsCampos() {
            // Arrange
            BeanPropertyBindingResult resultado = new BeanPropertyBindingResult(new Object(), "dto");
            resultado.addError(new FieldError("dto", "nome", "O nome e obrigatorio"));
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(resultado);

            // Act
            ResponseEntity<ErrorResponseDTO> resposta = handler.tratarValidacao(ex, request);

            // Assert
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resposta.getBody()).isNotNull();
            assertThat(resposta.getBody().detalhes()).containsExactly("O nome e obrigatorio");
            assertThat(resposta.getBody().timestamp()).isNotNull();
        }

        @Test
        @DisplayName("argumento invalido vira 400")
        void deveTratarArgumentoInvalido() {
            // Act
            ResponseEntity<ErrorResponseDTO> resposta =
                    handler.tratarArgumentoInvalido(new IllegalArgumentException("Parametro invalido"), request);

            // Assert
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resposta.getBody()).isNotNull();
            assertThat(resposta.getBody().mensagem()).isEqualTo("Parametro invalido");
        }

        @Test
        @DisplayName("corpo ilegivel vira 400, e nao erro interno")
        void deveTratarCorpoIlegivel() {
            // Arrange
            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "JSON parse error", new MockHttpInputMessage(new byte[0]));

            // Act
            ResponseEntity<ErrorResponseDTO> resposta = handler.tratarCorpoIlegivel(ex, request);

            // Assert: JSON quebrado e culpa de quem chamou; devolver 500 acusaria
            // falha do servidor e mandaria o time procurar defeito onde nao ha.
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resposta.getBody()).isNotNull();
            assertThat(resposta.getBody().erro()).isEqualTo("Corpo da requisicao invalido");
            assertThat(resposta.getBody().mensagem()).doesNotContain("JSON parse error");
        }

        @Test
        @DisplayName("parametro com tipo errado vira 400 nomeando o parametro")
        void deveTratarParametroComTipoErrado() {
            // Arrange
            MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                    "abc", Long.class, "id", mock(MethodParameter.class), new IllegalArgumentException());

            // Act
            ResponseEntity<ErrorResponseDTO> resposta = handler.tratarParametroComTipoErrado(ex, request);

            // Assert
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(resposta.getBody()).isNotNull();
            assertThat(resposta.getBody().erro()).isEqualTo("Parametro invalido");
            assertThat(resposta.getBody().mensagem()).contains("id");
        }
    }

    @Nested
    @DisplayName("Conflitos")
    class Conflitos {

        @Test
        @DisplayName("regra de negocio vira 409")
        void deveTratarRegraDeNegocio() {
            // Act
            ResponseEntity<ErrorResponseDTO> resposta =
                    handler.tratarRegraDeNegocio(new BusinessException("Telefone duplicado"), request);

            // Assert
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resposta.getBody()).isNotNull();
            assertThat(resposta.getBody().erro()).isEqualTo("Violacao de regra de negocio");
            assertThat(resposta.getBody().mensagem()).isEqualTo("Telefone duplicado");
        }

        @Test
        @DisplayName("violacao de integridade do banco vira 409")
        void deveTratarViolacaoDeIntegridade() {
            // Act
            ResponseEntity<ErrorResponseDTO> resposta =
                    handler.tratarViolacaoDeIntegridade(new DataIntegrityViolationException("unique"), request);

            // Assert
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resposta.getBody()).isNotNull();
            assertThat(resposta.getBody().erro()).isEqualTo("Violacao de integridade");
            assertThat(resposta.getBody().mensagem()).doesNotContain("unique");
        }
    }

    @Nested
    @DisplayName("Erro inesperado")
    class ErroInterno {

        @Test
        @DisplayName("vira 500 sem expor a mensagem interna")
        void deveTratarErroGenericoSemExporDetalhes() {
            // Act
            ResponseEntity<ErrorResponseDTO> resposta =
                    handler.tratarErroGenerico(new IllegalStateException("stack interna"), request);

            // Assert
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(resposta.getBody()).isNotNull();
            assertThat(resposta.getBody().mensagem()).doesNotContain("stack interna");
            assertThat(resposta.getBody().detalhes()).isEmpty();
        }
    }
}
