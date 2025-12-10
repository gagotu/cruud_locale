package com.exprivia.nest.cruud.mapper;

import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.model.Property;
import com.exprivia.nest.cruud.service.PropertyService;
import org.mapstruct.Mapper;

/**
 * Property Mapper to mapping Property and PropertyDto
 */
@Mapper(componentModel = "spring", uses = PropertyService.class)
public interface PropertyMapper {

    @org.mapstruct.Mapping(target = "createdAt", ignore = true)
    Property dtoToModel(PropertyDto dto);

    PropertyDto modelToDto(Property model);

}
