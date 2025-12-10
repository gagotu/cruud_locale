package com.exprivia.nest.cruud.mapper;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.model.Extraction;
import com.exprivia.nest.cruud.service.ExtractionService;
import org.mapstruct.Mapper;

/**
 * Extraction Event Mapper to map model and dto
 */
@Mapper(componentModel = "spring", uses = ExtractionService.class)
public interface ExtractionMapper {

    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    Extraction dtoToModel(ExtractionDto dto);

    ExtractionDto modelToDto(Extraction model);

}
