package com.exprivia.nest.cruud.repository;

import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.mapper.PropertyMapperImpl;
import com.exprivia.nest.cruud.model.Property;
import com.exprivia.nest.cruud.repository.impl.CustomPropertyRepositoryImpl;
import com.exprivia.nest.cruud.utils.PropertyBaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Property Repository Test Class
 */
@ExtendWith(SpringExtension.class)
@Import({PropertyMapperImpl.class, CustomPropertyRepositoryImpl.class})
public class PropertyRepositoryTest extends PropertyBaseTest {

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @Autowired
    private CustomPropertyRepositoryImpl propertyRepository;

    @BeforeEach
    void setUpRepositoryTest() {
        Mockito.when(mongoTemplate.find(Mockito.any(), Mockito.any())).thenReturn(List.of(propertyMapper.dtoToModel(propertyDto)));
    }

    @Test
    void testFilteredProperties() {
        PropertyFilterDto filterDto = PropertyFilterDto.builder()
                .ids(List.of(ID))
                .build();
        var result = convertListModelToDto(propertyRepository.getFilteredProperties(filterDto));

        assertEquals(List.of(propertyDto), result);
    }

    @Test
    void testFilteredPropertiesWithName() {
        PropertyFilterDto filterDto = PropertyFilterDto.builder()
                .propertiesName(List.of(ID))
                .build();

        var result = convertListModelToDto(propertyRepository.getFilteredProperties(filterDto));

        assertEquals(List.of(propertyDto), result);
    }

    private List<PropertyDto> convertListModelToDto(List<Property> list) {
        return list.stream()
                .map(dto -> {
                    dto.setCreatedAt(propertyDto.getCreatedAt());
                    return propertyMapper.modelToDto(dto);
                })
                .toList();
    }

}
