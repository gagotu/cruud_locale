package com.exprivia.nest.cruud.dto.urbandataset.values;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Contiene l'elenco delle righe di valori (line) dell'UrbanDataset.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValuesDto {

    private List<ResultValueDto> line;

}
