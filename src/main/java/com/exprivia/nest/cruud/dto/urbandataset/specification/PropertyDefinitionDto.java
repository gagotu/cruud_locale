package com.exprivia.nest.cruud.dto.urbandataset.specification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Collezione delle proprietà definite nella specifica UD.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDefinitionDto {

    private List<PropertiesDto> propertyDefinition;

}
