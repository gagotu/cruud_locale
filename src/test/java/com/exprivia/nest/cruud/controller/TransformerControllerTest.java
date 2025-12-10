package com.exprivia.nest.cruud.controller;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.dto.JSONDto;
import com.exprivia.nest.cruud.dto.RequestResultDto;
import com.exprivia.nest.cruud.dto.ResultUrbanDataset;
import com.exprivia.nest.cruud.dto.sourcedataset.DynamicValueDto;
import com.exprivia.nest.cruud.dto.sourcedataset.MetadataDto;
import com.exprivia.nest.cruud.dto.urbandataset.UrbanDatasetDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.IdDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.SpecificationDto;
import com.exprivia.nest.cruud.service.ExternalService;
import com.exprivia.nest.cruud.service.TransformerService;
import com.exprivia.nest.cruud.utils.Endpoints;
import com.exprivia.nest.cruud.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Transformer Controller Test Class
 */
@ExtendWith(SpringExtension.class)
@Import({TransformerController.class})
public class TransformerControllerTest {

    @Autowired
    private TransformerController transformerController;

    @MockitoBean
    private TransformerService transformerService;

    @MockitoBean
    private ExternalService externalService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUpControllerTest() {
        mockMvc = MockMvcBuilders.standaloneSetup(transformerController).build();
    }

    @Test
    void testExecuteConversionFromFolder() throws Exception {
        ExtractionDto mockDto = ExtractionDto.builder()
                .propertyName("test")
                .extractionName("test")
                .separator(';')
                .build();

        Mockito.when(transformerService.executeConversionFromFolder(Mockito.any(ExtractionDto.class)))
                .thenReturn(RequestResultDto.builder().description("ok").filesCompleted(List.of()).build());

        mockMvc.perform(MockMvcRequestBuilders
                        .post(Endpoints.TRANSFORMER + Endpoints.CSV)
                        .content(TestUtils.asJsonString(mockDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8"))
                .andExpect(status().isOk());
    }

    @Test
    void testExecuteConversionFromExtraction() throws Exception {
        Mockito.when(transformerService.executeConversionFromExtraction(Mockito.anyString()))
                .thenReturn(RequestResultDto.builder().description("ok").filesCompleted(List.of()).build());

        mockMvc.perform(MockMvcRequestBuilders
                        .get(Endpoints.TRANSFORMER + Endpoints.CSV + Endpoints.SLASH + "test")
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8"))
                .andExpect(status().isOk());
    }

    @Test
    void testExecuteConversionFromUpload() throws Exception {
        // Crea un MockMultipartFile per simulare il file
        MockMultipartFile file = new MockMultipartFile(
                "file",                 // Nome del parametro nel controller
                "test.csv",                    // Nome del file
                "text/csv",                    // Content type
                "id,name\n1,test".getBytes()    // Contenuto del file
        );

        Mockito.when(transformerService.executeConversionFromUpload(Mockito.any(), Mockito.anyString())).thenReturn(ResultUrbanDataset.builder().urbanDataset(UrbanDatasetDto.builder().build()).build());

        mockMvc.perform(MockMvcRequestBuilders.multipart(Endpoints.TRANSFORMER + Endpoints.SLASH + Endpoints.UPLOAD + Endpoints.SLASH + "ud1")
                        .file(file)
                        .param("propertyName", "testProperty")
                        .param("separator", ",")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @Test
    void testExecuteConversionFromOpenCruise() throws Exception {
        DynamicValueDto dynamicDto = new DynamicValueDto();
        dynamicDto.getAttributes().put("key", "value");
        dynamicDto.getAttributes().put("key2", 5);
        dynamicDto.getAttributes().put("key3", 55.12D);

        JSONDto jsonDto = JSONDto.builder()
                .metadata(List.of(MetadataDto.builder()
                        .name("name")
                        .description("description")
                        .type("type")
                        .example("example")
                        .unitOfMeasure("unitOfMeasure").build()))
                .result(List.of(dynamicDto))
                .build();

        Mockito.when(externalService.executeConversionFromOpenCruise(Mockito.any(JSONDto.class), Mockito.anyString()))
                .thenReturn(ResultUrbanDataset.builder().urbanDataset(UrbanDatasetDto.builder()
                        .specification(SpecificationDto.builder()
                                .id(IdDto.builder().schemeID("test").value("test").build())
                                .build())
                        .build()).build());

        mockMvc.perform(MockMvcRequestBuilders.post(Endpoints.TRANSFORMER + Endpoints.SLASH + Endpoints.EXTERNAL + Endpoints.OPEN_CRUISE + Endpoints.SLASH + "ud_test")
                        .content(TestUtils.asJsonString(jsonDto))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .characterEncoding("utf-8"))
                .andExpect(status().isOk());
    }

}
