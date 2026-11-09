package com.financeflow.usuario;

import com.financeflow.usuario.dto.RespostaUsuario;

public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static RespostaUsuario toResponse(Usuario usuario) {
        return new RespostaUsuario(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPapel(), usuario.getCriadoEm());
    }
}
