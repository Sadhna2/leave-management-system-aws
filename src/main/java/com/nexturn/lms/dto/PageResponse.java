package com.nexturn.lms.dto;
import java.util.List;
import org.springframework.data.domain.Page;
/** Stable paginated JSON contract independent of Spring's internal PageImpl serialization. */
public record PageResponse<T>(List<T> content, int number, int size, long totalElements, int totalPages) {
    /** Copies page metadata and content into the public API representation. */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
