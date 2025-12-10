package com.exprivia.nest.cruud.dto.sourcedataset;

import lombok.*;

/**
 * Object value dto that contain data and params to conversion value
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValueDto {

    @NonNull
    private String name;

    private String function;

    private String nameForNegative;

    private String alternativeValue;

    private Boolean coordinates;

}
