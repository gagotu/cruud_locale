package com.exprivia.nest.cruud.mapper;

import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.model.Property;
import com.exprivia.nest.cruud.service.PropertyService;
import org.mapstruct.Mapper;

/**
 * Mapper MapStruct per convertire tra modello Property e DTO.
 * Ignora createdAt in direzione DTO→model per lasciare la gestione al modello/DB.
 */
@Mapper(componentModel = "spring", uses = PropertyService.class)
public interface PropertyMapper {

    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    Property dtoToModel(PropertyDto dto);

    PropertyDto modelToDto(Property model);

}
