package com.exprivia.nest.cruud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO di filtro per selezionare property per id o per nome.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyFilterDto {

    private List<String> ids;

    private List<String> propertiesName;

}
