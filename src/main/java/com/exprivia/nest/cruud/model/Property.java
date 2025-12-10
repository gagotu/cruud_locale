package com.exprivia.nest.cruud.model;

import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.HashMap;

/**
 * Property model class
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Document("Properties")
public class Property {

    @Id
    private String id;

    @NonNull
    @Indexed(unique = true)
    private String name;

    private String description;

    private HashMap<String, Object> specification;

    private HashMap<String, Object> context;

    private HashMap<String, ValueDto> mappings;

    private HashMap<String, Object> configurations;

    @Builder.Default
    private Date createdAt = new Date();

}
