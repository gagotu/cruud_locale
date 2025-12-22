package com.exprivia.nest.cruud.dto.urbandataset.values;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Coppia nome/valore di una property nell'UrbanDataset.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
public class PropertyValueDto {

    private String name;

    private String val;

}
