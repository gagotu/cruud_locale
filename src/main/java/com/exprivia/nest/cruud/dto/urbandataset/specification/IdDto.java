package com.exprivia.nest.cruud.dto.urbandataset.specification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Identificativo della specifica UD (valore e schema di riferimento).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdDto {

    private String value;

    private String schemeID;

}
