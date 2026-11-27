package com.crmapi.sistemacrm.config;

import com.crmapi.sistemacrm.model.Usuario;
import com.crmapi.sistemacrm.model.enums.UsuarioRole;
import com.crmapi.sistemacrm.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdministradorInicial: primeiro ADMIN de uma instalacao nova")
class AdministradorInicialTest {

    private static final String EMAIL = "admin@crm.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private void executar(String email, String senha) {
        new AdministradorInicial(usuarioRepository, passwordEncoder, "Administrador", email, senha)
                .run(new DefaultApplicationArguments());
    }

    @Test
    @DisplayName("cria o ADMIN ativo, com a senha em hash, quando o banco nao tem nenhum")
    void deveCriarOAdministrador() {
        // Arrange
        when(usuarioRepository.existsByRole(UsuarioRole.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(passwordEncoder.encode("senha-forte")).thenReturn("$2a$10$hashDoAdmin");

        // Act
        executar(EMAIL, "senha-forte");

        // Assert
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(UsuarioRole.ADMIN);
        assertThat(captor.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(captor.getValue().getSenha()).isEqualTo("$2a$10$hashDoAdmin");
        assertThat(captor.getValue().getAtivo()).isTrue();
    }

    @Test
    @DisplayName("nao faz nada quando ADMIN_EMAIL ou ADMIN_PASSWORD nao foram definidos")
    void naoDeveCriarSemConfiguracao() {
        // Act
        executar("", "senha-forte");
        executar(EMAIL, "  ");

        // Assert
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("nao cria um segundo ADMIN quando ja existe um")
    void naoDeveDuplicarOAdministrador() {
        // Arrange
        when(usuarioRepository.existsByRole(UsuarioRole.ADMIN)).thenReturn(true);

        // Act
        executar(EMAIL, "senha-forte");

        // Assert
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("nao sobrescreve um usuario comum que ja usa o mesmo e-mail")
    void naoDeveSobrescreverUsuarioExistente() {
        // Arrange
        when(usuarioRepository.existsByRole(UsuarioRole.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);

        // Act
        executar(EMAIL, "senha-forte");

        // Assert
        verify(usuarioRepository, never()).save(any());
    }
}
