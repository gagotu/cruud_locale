package com.exprivia.nest.cruud.dto;

import com.exprivia.nest.cruud.dto.urbandataset.UrbanDatasetDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Wrapper usato per serializzare/deserializzare l'UrbanDataset completo nel payload finale.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultUrbanDataset {

    @NonNull
    @JsonProperty("UrbanDataset")
    private UrbanDatasetDto urbanDataset;

}
