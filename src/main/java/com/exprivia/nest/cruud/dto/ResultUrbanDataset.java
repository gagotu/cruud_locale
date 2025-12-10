package com.exprivia.nest.cruud.dto;

import com.exprivia.nest.cruud.dto.urbandataset.UrbanDatasetDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * Result Urban Dataset class.
 * With this class you can encapsulate a urban dataset
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
