package com.exprivia.nest.cruud.dto.urbandataset.specification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Descrive la specifica dell'UrbanDataset: id/versione/nome/URI e l'elenco
 * delle proprietà previste.
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
