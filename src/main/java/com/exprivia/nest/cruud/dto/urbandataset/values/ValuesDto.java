package com.exprivia.nest.cruud.dto.urbandataset.values;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Values class Dto for UD-Values
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValuesDto {

    private List<ResultValueDto> line;

}