package com.exprivia.nest.cruud.dto.urbandataset.specification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IdDto class for UD-specification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdDto {

    private String value;

    private String schemeID;

}
