package com.exprivia.nest.cruud.repository;

import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.model.Property;

import java.util.List;

/**
 * Custom Property Repository
 */
public interface CustomPropertyRepository {

    List<Property> getFilteredProperties(PropertyFilterDto dto);

}
