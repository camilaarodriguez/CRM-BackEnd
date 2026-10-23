package com.crmapi.sistemacrm.service.impl;

import com.crmapi.sistemacrm.dto.usuario.UsuarioCreateDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioSenhaDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioStatusDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioUpdateDTO;
import com.crmapi.sistemacrm.exception.BusinessException;
import com.crmapi.sistemacrm.exception.ResourceNotFoundException;
import com.crmapi.sistemacrm.mapper.UsuarioMapper;
import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Regras de gestao da equipe, com o repositorio mockado.
 *
 * O PasswordEncoder entra de verdade, e nao mockado: o que se quer provar nos
 * testes de senha e que o texto puro nunca chega ao objeto que vai para o banco,
 * e um encoder simulado tornaria essa prova vazia.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UsuarioServiceImpl: gestao da equipe")
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    private UsuarioServiceImpl usuarioService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final UsuarioCreateDTO criacao =
            new UsuarioCreateDTO("Ana Moraes", "ana@crm.com", "123456", UsuarioRole.VENDEDOR);

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioServiceImpl(usuarioRepository, new UsuarioMapper(), passwordEncoder);
    }

    private Usuario usuario(Long id, String email) {
        return Usuario.builder()
                .id(id)
                .nome("Ana Moraes")
                .email(email)
                .senha(new BCryptPasswordEncoder().encode("123456"))
                .role(UsuarioRole.VENDEDOR)
                .ativo(true)
                .build();
    }

    @Nested
    @DisplayName("Cadastro")
    class Criar {

        @Test
        @DisplayName("cadastra o usuario quando o e-mail esta livre")
        void deveCriarUsuarioQuandoEmailNaoExiste() {
            // Arrange
            when(usuarioRepository.existsByEmail("ana@crm.com")).thenReturn(false);
            when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuario(1L, "ana@crm.com"));

            // Act
            UsuarioResponseDTO resposta = usuarioService.criar(criacao);

            // Assert
            assertThat(resposta.id()).isEqualTo(1L);
            assertThat(resposta.email()).isEqualTo("ana@crm.com");
            assertThat(resposta.ativo()).isTrue();
        }

        @Test
        @DisplayName("recusa e-mail ja cadastrado sem tocar no banco")
        void naoDeveCriarUsuarioComEmailDuplicado() {
            // Arrange
            when(usuarioRepository.existsByEmail("ana@crm.com")).thenReturn(true);

            // Act + Assert
            assertThatThrownBy(() -> usuarioService.criar(criacao))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("email");
            verify(usuarioRepository, never()).save(any());
        }

        @Test
        @DisplayName("converte falha do banco em erro de negocio")
        void deveLancarErroDeNegocioQuandoFalhaAoSalvarNaCriacao() {
            // Arrange
            when(usuarioRepository.existsByEmail("ana@crm.com")).thenReturn(false);
            when(usuarioRepository.save(any(Usuario.class))).thenThrow(new RuntimeException("banco fora"));

            // Act + Assert
            assertThatThrownBy(() -> usuarioService.criar(criacao))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel criar o usuario");
        }
    }

    @Nested
    @DisplayName("Consultas")
    class Consultar {

        @Test
        @DisplayName("lista aplicando os filtros recebidos")
        void deveListarUsuariosComFiltros() {
            // Arrange
            Pageable pageable = PageRequest.of(0, 10);
            when(usuarioRepository.findAll(ArgumentMatchers.<Specification<Usuario>>any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(usuario(1L, "ana@crm.com"))));

            // Act
            Page<UsuarioResponseDTO> pagina = usuarioService.listar("ana", UsuarioRole.VENDEDOR, true, pageable);

            // Assert
            assertThat(pagina.getContent()).extracting(UsuarioResponseDTO::email).containsExactly("ana@crm.com");
        }

        @Test
        @DisplayName("devolve o usuario procurado")
        void deveBuscarUsuarioPorId() {
            // Arrange
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ana@crm.com")));

            // Act
            UsuarioResponseDTO resposta = usuarioService.buscarPorId(1L);

            // Assert
            assertThat(resposta.nome()).isEqualTo("Ana Moraes");
        }

        @Test
        @DisplayName("lanca nao encontrado citando o id procurado")
        void deveLancarErroQuandoUsuarioNaoEncontrado() {
            // Arrange
            when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

            // Act + Assert
            assertThatThrownBy(() -> usuarioService.buscarPorId(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    @Nested
    @DisplayName("Edicao")
    class Atualizar {

        @Test
        @DisplayName("grava nome, e-mail e perfil novos")
        void deveAtualizarUsuario() {
            // Arrange
            Usuario existente = usuario(1L, "ana@crm.com");
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO("Ana Beatriz", "ana.b@crm.com", UsuarioRole.GERENTE);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.findByEmail("ana.b@crm.com")).thenReturn(Optional.empty());
            when(usuarioRepository.save(existente)).thenReturn(existente);

            // Act
            UsuarioResponseDTO resposta = usuarioService.atualizar(1L, dto);

            // Assert
            assertThat(resposta.nome()).isEqualTo("Ana Beatriz");
            assertThat(resposta.role()).isEqualTo(UsuarioRole.GERENTE);
        }

        @Test
        @DisplayName("deixa salvar mantendo o proprio e-mail")
        void devePermitirAtualizarMantendoOProprioEmail() {
            // Arrange
            Usuario existente = usuario(1L, "ana@crm.com");
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO("Ana", "ana@crm.com", UsuarioRole.VENDEDOR);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.findByEmail("ana@crm.com")).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(existente)).thenReturn(existente);

            // Act
            UsuarioResponseDTO resposta = usuarioService.atualizar(1L, dto);

            // Assert: a checagem precisa reconhecer que o dono do e-mail e ele mesmo,
            // senao editar so o nome ficaria impossivel.
            assertThat(resposta.email()).isEqualTo("ana@crm.com");
        }

        @Test
        @DisplayName("recusa e-mail que ja e de outra pessoa")
        void naoDeveAtualizarComEmailDeOutroUsuario() {
            // Arrange
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO("Ana", "joao@crm.com", UsuarioRole.VENDEDOR);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ana@crm.com")));
            when(usuarioRepository.findByEmail("joao@crm.com")).thenReturn(Optional.of(usuario(2L, "joao@crm.com")));

            // Act + Assert
            assertThatThrownBy(() -> usuarioService.atualizar(1L, dto))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("converte falha do banco em erro de negocio")
        void deveLancarErroDeNegocioQuandoFalhaAoSalvarNaAtualizacao() {
            // Arrange
            Usuario existente = usuario(1L, "ana@crm.com");
            UsuarioUpdateDTO dto = new UsuarioUpdateDTO("Ana", "ana@crm.com", UsuarioRole.VENDEDOR);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.findByEmail("ana@crm.com")).thenReturn(Optional.empty());
            when(usuarioRepository.save(existente)).thenThrow(new RuntimeException("banco fora"));

            // Act + Assert
            assertThatThrownBy(() -> usuarioService.atualizar(1L, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel atualizar o usuario");
        }
    }

    @Nested
    @DisplayName("Ativacao e exclusao")
    class StatusEExclusao {

        @Test
        @DisplayName("desativa o usuario sem apagar o cadastro")
        void deveDesativarUsuario() {
            // Arrange
            Usuario existente = usuario(1L, "ana@crm.com");
            UsuarioStatusDTO dto = new UsuarioStatusDTO(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(existente)).thenReturn(existente);

            // Act
            UsuarioResponseDTO resposta = usuarioService.atualizarStatus(1L, dto);

            // Assert
            assertThat(resposta.ativo()).isFalse();
        }

        @Test
        @DisplayName("converte falha do banco em erro de negocio ao mudar o status")
        void deveLancarErroDeNegocioQuandoFalhaAoAtualizarStatus() {
            // Arrange
            Usuario existente = usuario(1L, "ana@crm.com");
            UsuarioStatusDTO dto = new UsuarioStatusDTO(false);
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(existente)).thenThrow(new RuntimeException("banco fora"));

            // Act + Assert
            assertThatThrownBy(() -> usuarioService.atualizarStatus(1L, dto))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel atualizar o status do usuario");
        }

        @Test
        @DisplayName("exclui o usuario")
        void deveDeletarUsuario() {
            // Arrange
            Usuario existente = usuario(1L, "ana@crm.com");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));

            // Act
            usuarioService.deletar(1L);

            // Assert
            verify(usuarioRepository).delete(existente);
        }

        @Test
        @DisplayName("converte o vinculo com clientes em erro de negocio")
        void deveLancarErroDeNegocioQuandoFalhaAoDeletar() {
            // Arrange
            Usuario existente = usuario(1L, "ana@crm.com");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            doThrow(new RuntimeException("vinculo")).when(usuarioRepository).delete(existente);

            // Act + Assert: um vendedor com carteira nao pode sumir e deixar clientes orfaos.
            assertThatThrownBy(() -> usuarioService.deletar(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Nao foi possivel deletar o usuario");
        }
    }

    @Nested
    @DisplayName("Tratamento da senha")
    class Senha {

        @Test
        @DisplayName("grava o hash BCrypt, nunca o texto digitado")
        void deveGravarSenhaComHashENuncaEmTextoPuro() {
            // Arrange
            when(usuarioRepository.existsByEmail("ana@crm.com")).thenReturn(false);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

            // Act
            usuarioService.criar(criacao);

            // Assert
            ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository).save(capturado.capture());
            String gravada = capturado.getValue().getSenha();

            assertThat(gravada).isNotEqualTo("123456").startsWith("$2a$");
            assertThat(passwordEncoder.matches("123456", gravada)).isTrue();
        }

        @Test
        @DisplayName("gera hashes diferentes para duas pessoas com a mesma senha")
        void deveGerarHashesDiferentesParaSenhasIguais() {
            // Arrange
            when(usuarioRepository.existsByEmail(ArgumentMatchers.anyString())).thenReturn(false);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

            // Act
            usuarioService.criar(criacao);
            usuarioService.criar(new UsuarioCreateDTO("Bia", "bia@crm.com", "123456", UsuarioRole.VENDEDOR));

            // Assert: o sal aleatorio impede que um vazamento denuncie senhas repetidas.
            ArgumentCaptor<Usuario> capturado = ArgumentCaptor.forClass(Usuario.class);
            verify(usuarioRepository, times(2)).save(capturado.capture());

            assertThat(capturado.getAllValues().get(0).getSenha())
                    .isNotEqualTo(capturado.getAllValues().get(1).getSenha());
        }

        @Test
        @DisplayName("troca a senha quando a atual confere")
        void deveAlterarSenhaQuandoAAtualConfere() {
            // Arrange
            Usuario existente = usuario(1L, "ana@crm.com");
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(chamada -> chamada.getArgument(0));

            // Act
            usuarioService.alterarSenha(1L, new UsuarioSenhaDTO("123456", "nova-senha-123"));

            // Assert
            assertThat(passwordEncoder.matches("nova-senha-123", existente.getSenha())).isTrue();
            assertThat(passwordEncoder.matches("123456", existente.getSenha())).isFalse();
        }

        @Test
        @DisplayName("recusa a troca quando a senha atual esta errada")
        void naoDeveAlterarSenhaQuandoAAtualNaoConfere() {
            // Arrange
            when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ana@crm.com")));

            UsuarioSenhaDTO troca = new UsuarioSenhaDTO("errada", "nova-senha-123");

            // Act + Assert: sem conferir a senha atual, quem pegasse a sessao aberta
            // de outra pessoa trocaria a senha dela.
            assertThatThrownBy(() -> usuarioService.alterarSenha(1L, troca))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("senha atual nao confere");

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }
    }
}
