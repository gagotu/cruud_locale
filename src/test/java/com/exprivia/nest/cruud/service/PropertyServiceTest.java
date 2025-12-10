package com.exprivia.nest.cruud.service;


import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.exception.DuplicateNameException;
import com.exprivia.nest.cruud.mapper.PropertyMapperImpl;
import com.exprivia.nest.cruud.model.Property;
import com.exprivia.nest.cruud.repository.PropertyRepository;
import com.exprivia.nest.cruud.utils.PropertyBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property Service Test class
 */
@ExtendWith(SpringExtension.class)
@Import({PropertyMapperImpl.class, PropertyService.class})
public class PropertyServiceTest extends PropertyBaseTest {

    @MockitoBean
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyService propertyService;

    @Test
    void createTest() throws DuplicateNameException {
        Mockito.when(propertyRepository.save(Mockito.any(Property.class))).thenReturn(propertyMapper.dtoToModel(propertyDto));

        var create = propertyService.create(propertyDto);
        create.setCreatedAt(propertyDto.getCreatedAt());

        assertEquals(create, propertyDto);
    }

    @Test
    void getAllTest() {
        Mockito.when(propertyRepository.findAll()).thenReturn(List.of(propertyMapper.dtoToModel(propertyDto)));

        var result = propertyService.getAll();
        result = result.stream().peek(dto -> dto.setCreatedAt(propertyDto.getCreatedAt())).toList();

        assertEquals(List.of(propertyDto), result);
    }

    @Test
    void getPropertyByIdTest() {
        Mockito.when(propertyRepository.findById(Mockito.anyString())).thenReturn(Optional.ofNullable(propertyMapper.dtoToModel(propertyDto)));

        var result = propertyService.getById(ID);

        result.ifPresent(dto -> {
            dto.setCreatedAt(propertyDto.getCreatedAt());
            assertEquals(propertyDto, dto);
        });
    }

    @Test
    void removePropertyById() {
        Mockito.doNothing().when(propertyRepository).deleteById(Mockito.anyString());

        propertyService.remove(ID);
    }

    @Test
    void getFilteredPropertiesTest() {
        PropertyFilterDto filterDto = PropertyFilterDto.builder().ids(List.of(ID)).build();

        Mockito.when(propertyRepository.getFilteredProperties(Mockito.any(PropertyFilterDto.class))).thenReturn(List.of(propertyMapper.dtoToModel(propertyDto)));

        var result = propertyService.getFilteredProperties(filterDto);
        result = result.stream().peek(dto -> dto.setCreatedAt(propertyDto.getCreatedAt())).toList();

        assertEquals(List.of(propertyDto), result);
    }

    @Test
    void testDtoToModel_NullDto_ReturnsNull() {
        // Act
        Property result = propertyMapper.dtoToModel(null);

        // Assert
        assertNull(result, "Se il DTO è null, il metodo deve restituire null.");
    }

    @Test
    void testDtoToModel_ValidDto_ReturnsProperty() {
        // Act
        Property result = propertyMapper.dtoToModel(propertyDto);

        // Assert
        assertNotNull(result);
        assertEquals(propertyDto.getId(), result.getId());
        assertEquals(propertyDto.getName(), result.getName());
        assertEquals(propertyDto.getDescription(), result.getDescription());
    }

}
