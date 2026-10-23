package com.crmapi.sistemacrm.repository.specification;

import com.crmapi.sistemacrm.model.Cliente;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.StatusFunil;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Montagem dos predicados, com o CriteriaBuilder mockado: o que se verifica aqui
 * e quais restricoes entram na consulta e quais ficam de fora quando o filtro vem
 * vazio. O efeito real da consulta - quem aparece e quem nao aparece na listagem -
 * e verificado em ClienteRepositoryTest, contra um banco de verdade.
 */
@SuppressWarnings("unchecked")
@DisplayName("Specifications de filtro")
class SpecificationsTest {

    private final CriteriaBuilder cb = mock(CriteriaBuilder.class);
    private final CriteriaQuery<Object> query = mock(CriteriaQuery.class);
    private final Predicate resultado = mock(Predicate.class);

    @BeforeEach
    void setUp() {
        when(cb.and(any(Predicate[].class))).thenReturn(resultado);
        // Um CriteriaBuilder de verdade nunca devolve null. O mock precisa fazer o
        // mesmo: e o LIKE de nome e o de e-mail que alimentam o OR da busca.
        when(cb.like(any(), anyString())).thenReturn(mock(Predicate.class));
        when(cb.or(any(Predicate.class), any(Predicate.class))).thenReturn(mock(Predicate.class));
    }

    @Nested
    @DisplayName("ClienteSpecification")
    class DeCliente {

        @Test
        @DisplayName("aplica busca, etapa, vendedor e o recorte de ativos")
        void clienteDeveAplicarTodosOsFiltros() {
            // Arrange
            Root<Cliente> root = mock(Root.class, RETURNS_DEEP_STUBS);

            // Act
            Predicate predicate = ClienteSpecification.comFiltros("Ana", StatusFunil.NEGOCIACAO, 3L, false)
                    .toPredicate(root, query, cb);

            // Assert: a busca vira dois LIKE, um por nome e um por e-mail.
            assertThat(predicate).isSameAs(resultado);
            verify(cb, times(2)).like(any(), eq("%ana%"));
            verify(cb).equal(any(), eq(StatusFunil.NEGOCIACAO));
            verify(cb).equal(any(), eq(3L));
            verify(cb).equal(any(), eq(true));
        }

        @Test
        @DisplayName("sem filtro e incluindo inativos nao restringe nada")
        void clienteSemFiltrosEIncluindoInativosNaoDeveRestringir() {
            // Arrange
            Root<Cliente> root = mock(Root.class, RETURNS_DEEP_STUBS);

            // Act
            Predicate predicate = ClienteSpecification.comFiltros(null, null, null, true)
                    .toPredicate(root, query, cb);

            // Assert
            assertThat(predicate).isSameAs(resultado);
            verify(cb, never()).like(any(), anyString());
            verify(cb, never()).equal(any(), any(Object.class));
        }

        @Test
        @DisplayName("busca so com espacos e ignorada, mas o recorte de ativos continua")
        void clienteComBuscaEmBrancoDeveTrazerApenasAtivos() {
            // Arrange
            Root<Cliente> root = mock(Root.class, RETURNS_DEEP_STUBS);

            // Act
            Predicate predicate = ClienteSpecification.comFiltros("  ", null, null, null)
                    .toPredicate(root, query, cb);

            // Assert
            assertThat(predicate).isSameAs(resultado);
            verify(cb, never()).like(any(), anyString());
            verify(cb).equal(any(), eq(true));
        }
    }

    @Nested
    @DisplayName("UsuarioSpecification")
    class DeUsuario {

        @Test
        @DisplayName("aplica busca, perfil e status")
        void usuarioDeveAplicarTodosOsFiltros() {
            // Arrange
            Root<Usuario> root = mock(Root.class, RETURNS_DEEP_STUBS);

            // Act
            Predicate predicate = UsuarioSpecification.comFiltros("ANA", UsuarioRole.GERENTE, false)
                    .toPredicate(root, query, cb);

            // Assert: a busca nao diferencia maiusculas de minusculas.
            assertThat(predicate).isSameAs(resultado);
            verify(cb, times(2)).like(any(), eq("%ana%"));
            verify(cb).equal(any(), eq(UsuarioRole.GERENTE));
            verify(cb).equal(any(), eq(false));
        }

        @Test
        @DisplayName("sem filtro nenhum nao restringe nada")
        void usuarioSemFiltrosNaoDeveRestringir() {
            // Arrange
            Root<Usuario> root = mock(Root.class, RETURNS_DEEP_STUBS);

            // Act
            Predicate predicate = UsuarioSpecification.comFiltros(" ", null, null)
                    .toPredicate(root, query, cb);

            // Assert: diferente do cliente, a listagem de equipe mostra inativos por padrao.
            assertThat(predicate).isSameAs(resultado);
            verify(cb, never()).like(any(), anyString());
            verify(cb, never()).equal(any(), any(Object.class));
        }
    }
}
