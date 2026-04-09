package com.casaglass.casaglass_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CatalogoProductosTrasladoResponseDTO {
    private Long sedeOrigenId;
    private List<CatalogoProductoTrasladoDTO> items;
    private Long totalElements;
    private Integer totalPages;
    private Integer page;
    private Integer size;
    private Boolean hasNext;
    private Boolean hasPrevious;
}
