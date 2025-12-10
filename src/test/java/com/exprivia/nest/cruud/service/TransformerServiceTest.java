package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.dto.RequestResultDto;
import com.exprivia.nest.cruud.dto.ResultUrbanDataset;
import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.context.CoordinatesDto;
import com.exprivia.nest.cruud.dto.urbandataset.context.ProducerDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.IdDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.PropertyDefinitionDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.PropertiesDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.SpecificationDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.SubPropertiesDto;
import com.exprivia.nest.cruud.service.TimeNormalizationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transformer Service Test Class
 */
@ExtendWith(SpringExtension.class)
@Import({TransformerService.class, ObjectMapper.class, TimeNormalizationService.class})
@TestPropertySource(locations = "classpath:application-test.properties")
class TransformerServiceTest {

    @Autowired
    private TransformerService transformerService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PropertyService propertyService;

    @MockitoBean
    private ExtractionService extractionService;

    private PropertyDto propertyDto;

    @TempDir
    Path tempDir;

    private Path sourceDir;
    private Path outputDir;

    @BeforeEach
    void setUp() throws IOException {
        sourceDir = Files.createDirectories(tempDir.resolve("source"));
        outputDir = Files.createDirectories(tempDir.resolve("output"));

        SpecificationDto specificationDto = SpecificationDto.builder()
                .name("User Electric Consumption")
                .version("2.0")
                .id(IdDto.builder().schemeID("SCPS").value("UserElectricConsumption-2.0").build())
                .uri("https://example.test/ontology/UserElectricConsumption")
                .build();

        HashMap<String, Object> specificationMap = new HashMap<>();
        specificationMap.put("name", specificationDto.getName());
        specificationMap.put("version", specificationDto.getVersion());
        specificationMap.put("id", specificationDto.getId());
        specificationMap.put("uri", specificationDto.getUri());
        specificationMap.put("properties", PropertyDefinitionDto.builder()
                .propertyDefinition(List.of(
                        PropertiesDto.builder()
                                .propertyName("EnergyConsumerID")
                                .propertyDescription("Identificativo utenza energetica")
                                .dataType("string")
                                .unitOfMeasure("dimensionless")
                                .build(),
                        PropertiesDto.builder()
                                .propertyName("period")
                                .propertyDescription("Intervallo di misura")
                                .subProperties(SubPropertiesDto.builder()
                                        .propertyName(List.of("start_ts", "end_ts"))
                                        .build())
                                .build(),
                        PropertiesDto.builder()
                                .propertyName("ElectricPower")
                                .propertyDescription("Potenza elettrica")
                                .dataType("double")
                                .unitOfMeasure("kilowatt")
                                .build(),
                        PropertiesDto.builder()
                                .propertyName("start_ts")
                                .propertyDescription("Inizio intervallo")
                                .dataType("dateTime")
                                .unitOfMeasure("dimensionless")
                                .build(),
                        PropertiesDto.builder()
                                .propertyName("end_ts")
                                .propertyDescription("Fine intervallo")
                                .dataType("dateTime")
                                .unitOfMeasure("dimensionless")
                                .build()
                ))
                .build());

        HashMap<String, Object> contextMap = new HashMap<>();
        contextMap.put("coordinates", CoordinatesDto.builder()
                .format("WGS84-DD")
                .latitude(0D)
                .longitude(0D)
                .height(0D)
                .build());
        contextMap.put("note", "");
        contextMap.put("timestamp", Instant.parse("2025-01-01T00:00:00Z").toString());
        contextMap.put("timeZone", "UTC+1");
        contextMap.put("language", "IT");
        contextMap.put("producer", ProducerDto.builder().id("Solution-ID").schemeID("SCPS").build());

        HashMap<String, ValueDto> mappings = new HashMap<>();
        mappings.put("entity_id", ValueDto.builder().name("EnergyConsumerID").build());
        mappings.put("state", ValueDto.builder().name("ElectricPower").function("num").alternativeValue("null").build());

        HashMap<String, Object> configurations = new HashMap<>();
        configurations.put("date", "last_changed");
        configurations.put("period", List.of("last_changed"));
        configurations.put("slice", 0);
        HashMap<String, String> nullsField = new HashMap<>();
        nullsField.put("ElectricPower", "null");
        configurations.put("nullsField", nullsField);

        propertyDto = PropertyDto.builder()
                .name("lab_load")
                .specification(specificationMap)
                .context(contextMap)
                .mappings(mappings)
                .configurations(configurations)
                .build();
    }

    @Test
    void testExecuteConversionFromFolder() throws IOException {
        Mockito.when(propertyService.getFilteredProperties(Mockito.any(PropertyFilterDto.class)))
                .thenReturn(List.of(propertyDto));

        Path csvPath = createSampleCsv("lab_load_mini.csv");

        ExtractionDto extraction = ExtractionDto.builder()
                .propertyName(propertyDto.getName())
                .extractionName("lab_load")
                .separator(',')
                .sourceFilesPath(sourceDir.toString())
                .outputFilesPath(outputDir.toString())
                .timeZone("Europe/Rome")
                .udUtc("+2")
                .handle(false)
                .build();

        Mockito.when(extractionService.getByExtractionName(Mockito.anyString())).thenReturn(Optional.of(extraction));

        RequestResultDto result = transformerService.executeConversionFromFolder(extraction);

        assertNotNull(result);
        assertTrue(Files.exists(sourceDir.resolve("completed").resolve(csvPath.getFileName())));
    }

    @Test
    void testExecuteConversionFromExtraction() throws IOException {
        Mockito.when(propertyService.getFilteredProperties(Mockito.any(PropertyFilterDto.class)))
                .thenReturn(List.of(propertyDto));

        createSampleCsv("lab_load_mini.csv");

        ExtractionDto extraction = ExtractionDto.builder()
                .propertyName(propertyDto.getName())
                .extractionName("lab_load")
                .separator(',')
                .sourceFilesPath(sourceDir.toString())
                .outputFilesPath(outputDir.toString())
                .timeZone("Europe/Rome")
                .udUtc("+2")
                .handle(false)
                .build();

        Mockito.when(extractionService.getByExtractionName(Mockito.anyString())).thenReturn(Optional.of(extraction));

        RequestResultDto result = transformerService.executeConversionFromExtraction("lab_load");

        assertNotNull(result);
    }

    @Test
    void testExecuteConversionFromUpload() throws IOException {
        Mockito.when(propertyService.getFilteredProperties(Mockito.any(PropertyFilterDto.class)))
                .thenReturn(List.of(propertyDto));

        ExtractionDto extraction = ExtractionDto.builder()
                .propertyName(propertyDto.getName())
                .extractionName("lab_load")
                .separator(',')
                .sourceFilesPath(sourceDir.toString())
                .outputFilesPath(outputDir.toString())
                .timeZone("Europe/Rome")
                .udUtc("+2")
                .handle(false)
                .build();

        Mockito.when(extractionService.getByExtractionName(Mockito.anyString())).thenReturn(Optional.of(extraction));

        String csvContent = """
                entity_id,state,last_changed
                sensor.lab_active_power,0.85,2023-10-23T08:25:23.292Z
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "lab_load.csv",
                "text/csv",
                csvContent.getBytes()
        );

        ResultUrbanDataset result = transformerService.executeConversionFromUpload(file, propertyDto.getName());

        assertNotNull(result);
    }

    private Path createSampleCsv(String fileName) throws IOException {
        String csvContent = """
                entity_id,state,last_changed
                sensor.lab_active_power,0.78,2023-10-23T08:19:23.532Z
                sensor.lab_active_power,0.76,2023-10-23T08:19:53.282Z
                """;
        Path csvPath = sourceDir.resolve(fileName);
        Files.writeString(csvPath, csvContent);
        return csvPath;
    }
}
