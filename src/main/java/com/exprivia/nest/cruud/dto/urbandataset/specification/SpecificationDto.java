package com.exprivia.nest.cruud.dto.urbandataset.specification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Specification Dto class for UD-specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpecificationDto {

    private String version;

    private IdDto id;

    private String name;

    private String uri;

    private PropertyDefinitionDto properties;

}
