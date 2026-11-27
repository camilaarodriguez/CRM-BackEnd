package com.crmapi.sistemacrm.service;

import com.crmapi.sistemacrm.dto.auth.LoginRequestDTO;
import com.crmapi.sistemacrm.dto.auth.LoginResponseDTO;
import com.crmapi.sistemacrm.dto.auth.RegistroRequestDTO;

public interface AuthService {

    /** Confere as credenciais e devolve um JWT para o usuario. */
    LoginResponseDTO autenticar(LoginRequestDTO dto);

    /** Cria a conta como VENDEDOR e ja devolve o JWT, sem exigir um segundo login. */
    LoginResponseDTO registrar(RegistroRequestDTO dto);
}
