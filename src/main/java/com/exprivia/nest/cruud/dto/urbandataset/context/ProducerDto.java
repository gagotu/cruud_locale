package com.exprivia.nest.cruud.dto.urbandataset.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Identificativo del produttore dell'UrbanDataset (id e schemeID).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProducerDto {

    private String id;

    private String schemeID;

}
