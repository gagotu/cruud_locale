package com.exprivia.nest.cruud.dto.urbandataset;

import com.exprivia.nest.cruud.dto.urbandataset.context.ContextDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.SpecificationDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.ValuesDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Urban Dataset Dto class
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrbanDatasetDto {

    private SpecificationDto specification;

    private ContextDto context;

    private ValuesDto values;

}
