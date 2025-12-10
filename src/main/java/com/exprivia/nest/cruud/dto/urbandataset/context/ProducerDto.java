package com.exprivia.nest.cruud.dto.urbandataset.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Producer Dto class for UD-context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProducerDto {

    private String id;

    private String schemeID;

}
