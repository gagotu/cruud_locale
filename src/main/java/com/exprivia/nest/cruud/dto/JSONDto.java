package com.exprivia.nest.cruud.dto;

import com.exprivia.nest.cruud.dto.sourcedataset.MetadataDto;
import com.exprivia.nest.cruud.dto.sourcedataset.DynamicValueDto;
import lombok.*;

import java.util.List;

/**
 * Rappresenta il payload JSON in ingresso proveniente da sorgenti esterne:
 * metadata descrittivi e valori dinamici da convertire.
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
