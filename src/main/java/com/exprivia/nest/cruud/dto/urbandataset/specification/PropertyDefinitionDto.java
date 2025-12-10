package com.exprivia.nest.cruud.dto.urbandataset.specification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Property Definition Dto class for UD-specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDefinitionDto {

    private List<PropertiesDto> propertyDefinition;

}
