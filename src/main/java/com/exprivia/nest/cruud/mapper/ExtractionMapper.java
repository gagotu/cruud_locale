package com.exprivia.nest.cruud.mapper;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.model.Extraction;
import com.exprivia.nest.cruud.service.ExtractionService;
import org.mapstruct.Mapper;

/**
 * Mapper MapStruct per convertire tra modello Extraction e DTO.
 * Ignora createdAt in direzione DTO→model per lasciare che sia il modello a gestire la data.
 */
@Mapper(componentModel = "spring", uses = ExtractionService.class)
public interface ExtractionMapper {

    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    Extraction dtoToModel(ExtractionDto dto);

    ExtractionDto modelToDto(Extraction model);

}
