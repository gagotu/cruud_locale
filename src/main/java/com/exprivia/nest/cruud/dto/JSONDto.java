package com.exprivia.nest.cruud.dto;

import com.exprivia.nest.cruud.dto.sourcedataset.MetadataDto;
import com.exprivia.nest.cruud.dto.sourcedataset.DynamicValueDto;
import lombok.*;

import java.util.List;

/**
 * class to define input JSON
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JSONDto {

    @NonNull
    private List<MetadataDto> metadata;

    @NonNull
    private List<DynamicValueDto> result;

}
