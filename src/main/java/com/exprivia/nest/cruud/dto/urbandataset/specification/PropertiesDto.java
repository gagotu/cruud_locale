package com.exprivia.nest.cruud.dto.urbandataset.specification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Properties Dto class for UD-specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertiesDto {

    private String propertyName;

    private String propertyDescription;

    private String dataType;

    private String unitOfMeasure;

    private String measurementType;

    private SubPropertiesDto subProperties;

}
