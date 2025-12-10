package com.exprivia.nest.cruud.controller;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.mapper.ExtractionMapperImpl;
import com.exprivia.nest.cruud.model.Extraction;
import com.exprivia.nest.cruud.repository.ExtractionRepository;
import com.exprivia.nest.cruud.service.ExtractionService;
import com.exprivia.nest.cruud.utils.Endpoints;
import com.exprivia.nest.cruud.utils.ExtractionBaseTest;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Extraction Controller Test class
 */
@ExtendWith(SpringExtension.class)
@Import({ExtractionMapperImpl.class, ExtractionController.class})
public class ExtractionControllerTest extends ExtractionBaseTest {

    @MockitoBean
    private ExtractionService extractionService;

    @MockitoBean
    private ExtractionRepository extractionRepository;

    @Autowired
    private ExtractionController extractionController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUpExtractionTest() {
        mockMvc = MockMvcBuilders.standaloneSetup(extractionController).build();
    }

    @Test
    void testCreateExtraction() throws Exception {
        Mockito.when(extractionRepository.save(Mockito.any(Extraction.class))).thenReturn(extractionMapper.dtoToModel(extractionDto));

        mockMvc.perform(MockMvcRequestBuilders
                .post(Endpoints.EXTRACTION)
                .content(TestUtils.asJsonString(extractionDto))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .characterEncoding("utf-8"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetAllExtractions() throws Exception {
        List<ExtractionDto> extractions = new ArrayList<>();
        extractions.add(extractionDto);

        Mockito.when(extractionRepository.findAll()).thenReturn(extractions.stream().map(extractionMapper::dtoToModel).toList());

        mockMvc.perform(MockMvcRequestBuilders
                .get(Endpoints.EXTRACTION + Endpoints.ALL))
                .andExpect(status().isOk());
    }

    @Test
    void testGetById() throws Exception {
        Mockito.when(extractionRepository.findById(Mockito.anyString())).thenReturn(Optional.ofNullable(extractionMapper.dtoToModel(extractionDto)));

        mockMvc.perform(MockMvcRequestBuilders
                .get(Endpoints.EXTRACTION + Endpoints.SLASH + "{id}", "id")
                .param("id", extractionDto.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void testRemove() throws Exception {
        Mockito.doNothing().when(extractionRepository).deleteById(Mockito.anyString());

        mockMvc.perform(MockMvcRequestBuilders
                .delete(Endpoints.EXTRACTION + Endpoints.DELETE + Endpoints.SLASH + "id", "id")
                .param("id", extractionDto.getId()))
                .andExpect(status().isOk());
    }

}
