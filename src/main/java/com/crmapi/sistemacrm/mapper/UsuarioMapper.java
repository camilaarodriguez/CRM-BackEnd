package com.crmapi.sistemacrm.mapper;

import com.crmapi.sistemacrm.dto.usuario.UsuarioCreateDTO;
import com.crmapi.sistemacrm.dto.usuario.UsuarioResponseDTO;
import com.crmapi.sistemacrm.model.Usuario;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    /**
     * A senha chega aqui ja codificada: o hash e responsabilidade da camada de
     * servico, para que nenhum caminho de criacao grave texto puro por descuido.
     */
    public Usuario paraEntidade(UsuarioCreateDTO dto, String senhaCodificada) {
        return Usuario.builder()
                .nome(dto.nome())
                .email(dto.email())
                .senha(senhaCodificada)
                .role(dto.role())
                .ativo(true)
                .build();
    }

    public UsuarioResponseDTO paraResponseDTO(Usuario usuario) {
        return new UsuarioResponseDTO(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getAtivo(),
                usuario.getCriadoEm(),
                usuario.getAtualizadoEm()
        );
    }
}
