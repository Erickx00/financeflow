package com.financeflow.usuario.dto;

import com.financeflow.usuario.Papel;

import java.time.Instant;
import java.util.UUID;

public record RespostaUsuario(UUID id, String nome, String email, Papel papel, Instant criadoEm) {
}
