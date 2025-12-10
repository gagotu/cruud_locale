package com.exprivia.nest.cruud.dto;

import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import lombok.*;

import java.util.Date;
import java.util.HashMap;

/**
 * Property dto class
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyDto {

    private String id;

    @NonNull
    private String name;

    private String description;

    private HashMap<String, Object> specification;

    private HashMap<String, Object> context;

    private HashMap<String, ValueDto> mappings;

    private HashMap<String, Object> configurations;

    private Date createdAt;

}
