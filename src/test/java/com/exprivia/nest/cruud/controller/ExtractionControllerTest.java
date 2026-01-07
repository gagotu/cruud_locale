package com.exprivia.nest.cruud.controller;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.exception.handler.GlobalExceptionHandler;
import com.exprivia.nest.cruud.mapper.ExtractionMapperImpl;
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

    @Autowired
    private ExtractionController extractionController;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUpExtractionTest() {
        mockMvc = MockMvcBuilders.standaloneSetup(extractionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testCreateExtraction() throws Exception {
        Mockito.when(extractionService.create(Mockito.any(ExtractionDto.class))).thenReturn(extractionDto);

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

        Mockito.when(extractionService.getAll()).thenReturn(extractions);

        mockMvc.perform(MockMvcRequestBuilders
                .get(Endpoints.EXTRACTION + Endpoints.ALL))
                .andExpect(status().isOk());
    }

    @Test
    void testGetById() throws Exception {
        Mockito.when(extractionService.getById(Mockito.anyString())).thenReturn(Optional.of(extractionDto));

        mockMvc.perform(MockMvcRequestBuilders
                .get(Endpoints.EXTRACTION + Endpoints.SLASH + "{id}", "id")
                .param("id", extractionDto.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void testRemove() throws Exception {
        Mockito.doNothing().when(extractionService).remove(Mockito.anyString());

        mockMvc.perform(MockMvcRequestBuilders
                .delete(Endpoints.EXTRACTION + Endpoints.DELETE + Endpoints.SLASH + "id", "id")
                .param("id", extractionDto.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        Mockito.when(extractionService.getById(Mockito.anyString())).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders
                        .get(Endpoints.EXTRACTION + Endpoints.SLASH + "{id}", "missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetByExtractionNameNotFound() throws Exception {
        Mockito.when(extractionService.getByExtractionName(Mockito.anyString())).thenReturn(Optional.empty());

        mockMvc.perform(MockMvcRequestBuilders
                        .get(Endpoints.EXTRACTION + Endpoints.NAME + Endpoints.SLASH + "{extractionName}", "missing"))
                .andExpect(status().isNotFound());
    }

}
