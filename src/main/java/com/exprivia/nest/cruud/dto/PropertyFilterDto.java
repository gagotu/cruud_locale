package com.exprivia.nest.cruud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Property Filter Dto class
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyFilterDto {

    private List<String> ids;

    private List<String> propertiesName;

}
