package com.exprivia.nest.cruud.controller;

import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.mapper.PropertyMapperImpl;
import com.exprivia.nest.cruud.model.Property;
import com.exprivia.nest.cruud.repository.PropertyRepository;
import com.exprivia.nest.cruud.service.PropertyService;
import com.exprivia.nest.cruud.utils.Endpoints;
import com.exprivia.nest.cruud.utils.PropertyBaseTest;
import com.exprivia.nest.cruud.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Property Controller test class
 */
@ExtendWith(SpringExtension.class)
@Import({PropertyMapperImpl.class, PropertyController.class})
public class PropertyControllerTest extends PropertyBaseTest {

    @MockitoBean
    private PropertyService propertyService;

    @MockitoBean
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyController propertyController;

    private MockMvc mockMvc;

    /**
     * Setup controller test public constructor
     */
    @BeforeEach
    public void setUpControllerTest() {
        mockMvc = MockMvcBuilders.standaloneSetup(propertyController).build();
    }

    @Test
    void testCreateProperty() throws Exception {
        Mockito.when(propertyRepository.save(Mockito.any(Property.class))).thenReturn(propertyMapper.dtoToModel(propertyDto));

        mockMvc.perform(MockMvcRequestBuilders
                .post(Endpoints.PROPERTY)
                .content(TestUtils.asJsonString(propertyDto))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllProperties() throws Exception {
        List<PropertyDto> properties = new ArrayList<>();
        properties.add(propertyDto);

        Mockito.when(propertyRepository.findAll()).thenReturn(properties.stream().map(propertyMapper::dtoToModel).toList());

        mockMvc.perform(MockMvcRequestBuilders
                .get(Endpoints.PROPERTY + Endpoints.ALL))
                .andExpect(status().isOk());
    }

    @Test
    void testGetById() throws Exception {
        Mockito.when(propertyRepository.findById(Mockito.anyString())).thenReturn(Optional.ofNullable(propertyMapper.dtoToModel(propertyDto)));

        mockMvc.perform(MockMvcRequestBuilders
                .get(Endpoints.PROPERTY + Endpoints.SLASH + "{id}", "id")
                        .param("id", propertyDto.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void testRemove() throws Exception {
        Mockito.doNothing().when(propertyRepository).deleteById(Mockito.anyString());

        mockMvc.perform(MockMvcRequestBuilders
                .delete(Endpoints.PROPERTY + Endpoints.DELETE + Endpoints.SLASH + "id", "id")
                .param("id", propertyDto.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetFilteredProperties() throws Exception {
        List<PropertyDto> properties = new ArrayList<>();
        properties.add(propertyDto);

        PropertyFilterDto propertyFilterDto = PropertyFilterDto.builder().ids(List.of(ID)).build();

        Mockito.when(propertyRepository.getFilteredProperties(Mockito.any(PropertyFilterDto.class))).thenReturn(properties.stream().map(propertyMapper::dtoToModel).toList());

        mockMvc.perform(MockMvcRequestBuilders
                        .post(Endpoints.PROPERTY + Endpoints.FILTER)
                        .content(TestUtils.asJsonString(propertyFilterDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8"))
                .andExpect(status().isOk());
    }

}
