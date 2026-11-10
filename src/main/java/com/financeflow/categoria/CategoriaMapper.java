package com.financeflow.categoria;

import com.financeflow.categoria.dto.RespostaCategoria;

public final class CategoriaMapper {

    private CategoriaMapper() {
    }

    public static RespostaCategoria toResponse(Categoria categoria) {
        return new RespostaCategoria(categoria.getId(), categoria.getNome(), categoria.getTipo(),
                categoria.getCor(), categoria.getCriadoEm());
    }
}
