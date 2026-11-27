package com.crmapi.sistemacrm.controller;

import com.crmapi.sistemacrm.dto.auth.LoginRequestDTO;
import com.crmapi.sistemacrm.dto.auth.LoginResponseDTO;
import com.crmapi.sistemacrm.dto.auth.RegistroRequestDTO;
import com.crmapi.sistemacrm.exception.ErrorResponseDTO;
import com.crmapi.sistemacrm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@SecurityRequirements
@Tag(name = "Autenticacao", description = "Login e cadastro publico; as duas rotas devolvem o JWT")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Autentica um usuario",
            description = """
                    Confere a senha informada contra o hash BCrypt guardado e devolve um JWT com
                    data de expiracao, junto dos dados do usuario (sem a senha). E-mail inexistente
                    e senha errada produzem a mesma resposta, para nao revelar quais e-mails estao
                    cadastrados.""")
    @ApiResponse(responseCode = "200", description = "Autenticado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "E-mail ou senha ausentes ou em formato invalido",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(responseCode = "401", description = "E-mail ou senha invalidos",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(responseCode = "403", description = "Usuario inativo",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponseDTO.class)))
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginRequestDTO dto) {
        return ResponseEntity.ok(authService.autenticar(dto));
    }

    @Operation(
            summary = "Cria uma conta",
            description = """
                    Cadastro aberto ao publico. A conta nasce ativa e sempre com o perfil VENDEDOR;
                    a promocao a GERENTE ou ADMIN e feita pela gestao de equipe. Devolve o JWT, entao
                    a pessoa ja entra no sistema sem precisar fazer login em seguida.""")
    @ApiResponse(responseCode = "201", description = "Conta criada",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = LoginResponseDTO.class)))
    @ApiResponse(responseCode = "400", description = "Campos obrigatorios ausentes ou invalidos",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponseDTO.class)))
    @ApiResponse(responseCode = "409", description = "Ja existe um usuario com o e-mail informado",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponseDTO.class)))
    @PostMapping("/registro")
    public ResponseEntity<LoginResponseDTO> registrar(@Valid @RequestBody RegistroRequestDTO dto) {
        LoginResponseDTO criado = authService.registrar(dto);
        return ResponseEntity.created(URI.create("/api/usuarios/" + criado.usuario().id())).body(criado);
    }
}
