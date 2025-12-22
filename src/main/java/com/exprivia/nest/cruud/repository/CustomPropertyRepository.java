package com.exprivia.nest.cruud.repository;

import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.model.Property;

import java.util.List;

/**
 * Repository custom per le property: applica filtri dinamici sulle proprietà
 * in base ai criteri del PropertyFilterDto.
 */
public interface CustomPropertyRepository {

    List<Property> getFilteredProperties(PropertyFilterDto dto);

}
