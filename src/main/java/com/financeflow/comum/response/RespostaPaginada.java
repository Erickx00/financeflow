package com.financeflow.comum.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Envelope de paginacao estavel, independente do tipo de `Page` do Spring. */
public record RespostaPaginada<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <E, T> RespostaPaginada<T> from(Page<E> page, Function<E, T> mapper) {
        return new RespostaPaginada<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
