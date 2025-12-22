package com.exprivia.nest.cruud.dto.urbandataset.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Coordinate spaziali del contesto UD (formato, latitudine, longitudine, altezza).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoordinatesDto {

    private String format;

    private Double latitude;

    private Double longitude;

    private Double height;

}
