package com.exprivia.nest.cruud.dto.urbandataset.specification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Elenco delle sotto-proprietà associate a una property (es. start_ts/end_ts per un periodo).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubPropertiesDto {

    private List<String> propertyName;

}
