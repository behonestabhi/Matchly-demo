package com.matchly.job.dto;

import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Uniform paginated response shape (API_CONTRACTS conventions):
 * {@code { content:[...], page:{number,size,totalElements,totalPages} }}.
 */
public record PageResponse<T>(List<T> content, PageMeta page) {

    public record PageMeta(int number, int size, long totalElements, int totalPages) {
    }

    /** Map a Spring {@link Page} of entities to a {@code PageResponse} of DTOs. */
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        List<T> content = page.getContent().stream().map(mapper).toList();
        return new PageResponse<>(content, new PageMeta(
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()));
    }
}
