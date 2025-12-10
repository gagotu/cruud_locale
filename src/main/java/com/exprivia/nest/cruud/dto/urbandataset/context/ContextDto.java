package com.exprivia.nest.cruud.dto.urbandataset.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Context Dto class for UD-context
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContextDto {

    private ProducerDto producer;

    private String timeZone;

    @Builder.Default
    private String timestamp = new Date().toString();

    private CoordinatesDto coordinates;

    private String language;

    @Builder.Default
    private String note = "";

}
