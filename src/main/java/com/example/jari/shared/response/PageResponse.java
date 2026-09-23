package com.example.jari.shared.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

@Getter
public class PageResponse<T> {

    private final List<T> data;
    private final int page;
    private final int size;
    private final long total;
    private final int totalPages;

    @JsonCreator
    private PageResponse(
            @JsonProperty("data") List<T> data,
            @JsonProperty("page") int page,
            @JsonProperty("size") int size,
            @JsonProperty("total") long total,
            @JsonProperty("totalPages") int totalPages) {
        this.data = data == null ? List.of() : new ArrayList<>(data);
        this.page = page;
        this.size = size;
        this.total = total;
        this.totalPages = totalPages;
    }

    private PageResponse(Page<T> page) {
        this(new ArrayList<>(page.getContent()), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page);
    }
}
