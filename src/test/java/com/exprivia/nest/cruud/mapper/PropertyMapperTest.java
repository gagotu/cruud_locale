package com.exprivia.nest.cruud.mapper;

import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.model.Property;
import com.exprivia.nest.cruud.utils.PropertyBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property Mapper Test Class
 */
@ExtendWith(SpringExtension.class)
@Import({PropertyMapperImpl.class})
public class PropertyMapperTest extends PropertyBaseTest {

    @Test
    void testDtoToModel() {
        Property result = propertyMapper.dtoToModel(propertyDto);
        result.setCreatedAt(propertyDto.getCreatedAt());

        assertEquals(propertyMapper.modelToDto(result), propertyDto);
    }

    @Test
    void testModelToDto() {
        Property test = Property.builder()
                .id(propertyDto.getId())
                .mappings(propertyDto.getMappings())
                .specification(propertyDto.getSpecification())
                .context(propertyDto.getContext())
                .name(propertyDto.getName())
                .description(propertyDto.getDescription())
                .createdAt(propertyDto.getCreatedAt())
                .configurations(propertyDto.getConfigurations())
                .build();

        PropertyDto result = propertyMapper.modelToDto(test);
        result.setCreatedAt(propertyDto.getCreatedAt());

        assertEquals(result, propertyDto);
    }

}
