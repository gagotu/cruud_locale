package com.exprivia.nest.cruud.dto.urbandataset.specification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * SubProperties Dto class for UD-specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubPropertiesDto {

    private List<String> propertyName;

}
