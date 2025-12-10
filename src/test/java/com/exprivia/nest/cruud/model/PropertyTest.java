package com.exprivia.nest.cruud.model;

import com.exprivia.nest.cruud.mapper.PropertyMapperImpl;
import com.exprivia.nest.cruud.utils.PropertyBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property Model Test Class
 */
@ExtendWith(SpringExtension.class)
@Import({PropertyMapperImpl.class})
public class PropertyTest extends PropertyBaseTest {

    @Test
    void testCreationObject() {
        Property property = Property.builder()
                .id(propertyDto.getId())
                .name(propertyDto.getName())
                .specification(propertyDto.getSpecification())
                .context(propertyDto.getContext())
                .description(propertyDto.getDescription())
                .createdAt(propertyDto.getCreatedAt())
                .mappings(propertyDto.getMappings())
                .configurations(propertyDto.getConfigurations())
                .build();

        assertEquals(propertyMapper.modelToDto(property), propertyDto);
    }

}
