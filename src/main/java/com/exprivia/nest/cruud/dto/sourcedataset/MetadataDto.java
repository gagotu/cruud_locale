package com.exprivia.nest.cruud.dto.sourcedataset;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadati associati alle colonne/attributi del dataset sorgente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetadataDto {

    private String unitOfMeasure;

    private String name;

    private String description;

    private String type;

    private String example;

}
