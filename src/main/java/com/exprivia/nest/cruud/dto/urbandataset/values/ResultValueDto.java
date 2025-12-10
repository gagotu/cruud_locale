package com.exprivia.nest.cruud.dto.urbandataset.values;

import com.exprivia.nest.cruud.dto.urbandataset.context.CoordinatesDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashMap;
import java.util.List;

/**
 * Result Value Dto for UD-values
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultValueDto {

    private int id;

    @JsonProperty("period")
    private HashMap<String, Object> period;

    private List<PropertyValueDto> property;

    private CoordinatesDto coordinates;

    private Object timestamp;

}
