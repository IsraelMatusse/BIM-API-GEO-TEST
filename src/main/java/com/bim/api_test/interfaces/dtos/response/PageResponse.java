package com.bim.api_test.interfaces.dtos.response;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Envelope de paginação da API.
 *
 * <p>Serializar {@code Page}/{@code PageImpl} directamente expõe a estrutura interna do Spring
 * Data, que já mudou entre versões e não é um contrato estável. Este record fixa o formato
 * devolvido aos clientes.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
