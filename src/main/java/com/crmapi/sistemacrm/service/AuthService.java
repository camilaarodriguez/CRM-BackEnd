package com.crmapi.sistemacrm.service;

import com.crmapi.sistemacrm.dto.auth.LoginRequestDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;

public interface AuthService {

    UsuarioResponseDTO autenticar(LoginRequestDTO dto);
}
