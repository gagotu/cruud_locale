package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.dto.ResultUrbanDataset;
import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.ResultValueDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@Import({TransformerService.class, ObjectMapper.class, TimeNormalizationService.class})
@TestPropertySource(properties = {
        "ud.maxRowsPerUd=2",
        "temp-folder=src/test/resources/temp/"
})
class TransformerServiceSplitTest {

    @Autowired
    private TransformerService transformerService;

    @MockitoBean
    private PropertyService propertyService;

    @MockitoBean
    private ExtractionService extractionService;

    @Autowired
    private ObjectMapper objectMapper;

    @TempDir
    Path tempDir;

    private Path sourceDir;
    private Path outputDir;
    private PropertyDto propertyDto;

    @BeforeEach
    void setUp() throws IOException {
        sourceDir = Files.createDirectories(tempDir.resolve("source"));
        outputDir = Files.createDirectories(tempDir.resolve("output"));

        HashMap<String, ValueDto> mappings = new HashMap<>();
        mappings.put("entity_id", ValueDto.builder().name("EnergyConsumerID").build());
        mappings.put("state", ValueDto.builder().name("ElectricPower").function("num").alternativeValue("null").build());

        HashMap<String, Object> configurations = new HashMap<>();
        configurations.put("date", "last_changed");
        configurations.put("period", List.of("last_changed"));
        configurations.put("slice", 0);
        configurations.put("nullsField", Map.of("ElectricPower", "null"));

        propertyDto = PropertyDto.builder()
                .name("lab_load")
                .specification(baseSpecification())
                .context(baseContext())
                .mappings(mappings)
                .configurations(configurations)
                .build();
    }

    @Test
    void splitOutputResetsIdsPerFile() throws Exception {
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

        Mockito.when(extractionService.getByExtractionName(Mockito.anyString()))
                .thenReturn(Optional.of(extraction));

        createSampleCsv("lab_load_split.csv");

        transformerService.executeConversionFromFolder(extraction);

        List<Path> outputs;
        try (var files = Files.list(outputDir)) {
            outputs = files.filter(p -> p.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        }

        assertEquals(3, outputs.size());

        for (Path path : outputs) {
            ResultUrbanDataset ud = objectMapper.readValue(path.toFile(), ResultUrbanDataset.class);
            List<?> lines = ud.getUrbanDataset().getValues().getLine();
            assertTrue(lines.size() > 0);
            int expectedId = 1;
            for (Object lineObj : lines) {
                int actualId = ((ResultValueDto) lineObj).getId();
                assertEquals(expectedId++, actualId);
            }
        }
    }

    private void createSampleCsv(String fileName) throws IOException {
        String csvContent = """
                entity_id,state,last_changed
                sensor.lab_active_power,0.78,2023-10-23T08:19:23.532Z
                sensor.lab_active_power,0.76,2023-10-23T08:19:53.282Z
                sensor.lab_active_power,0.75,2023-10-23T08:20:53.282Z
                sensor.lab_active_power,0.74,2023-10-23T08:21:53.282Z
                sensor.lab_active_power,0.73,2023-10-23T08:22:53.282Z
                """;
        Path csvPath = sourceDir.resolve(fileName);
        Files.writeString(csvPath, csvContent);
    }

    private HashMap<String, Object> baseSpecification() {
        HashMap<String, Object> specification = new HashMap<>();
        HashMap<String, Object> id = new HashMap<>();
        id.put("value", "test-id");
        id.put("schemeID", "SCPS");
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("propertyDefinition", List.of(
                specProperty("EnergyConsumerID", "string"),
                specSubPropertyPeriod(),
                specProperty("ElectricPower", "double"),
                specProperty("start_ts", "dateTime"),
                specProperty("end_ts", "dateTime")
        ));
        specification.put("properties", properties);
        specification.put("id", id);
        specification.put("name", "test");
        specification.put("version", "1.0");
        specification.put("uri", "test-uri");
        return specification;
    }

    private HashMap<String, Object> baseContext() {
        HashMap<String, Object> context = new HashMap<>();
        HashMap<String, Object> producer = new HashMap<>();
        producer.put("id", "Solution-ID");
        producer.put("schemeID", "SCPS");
        context.put("producer", producer);
        context.put("timeZone", "UTC+1");
        return context;
    }

    private HashMap<String, Object> specProperty(String name, String dataType) {
        HashMap<String, Object> prop = new HashMap<>();
        prop.put("propertyName", name);
        prop.put("propertyDescription", name);
        prop.put("dataType", dataType);
        prop.put("unitOfMeasure", "dimensionless");
        return prop;
    }

    private HashMap<String, Object> specSubPropertyPeriod() {
        HashMap<String, Object> period = new HashMap<>();
        period.put("propertyName", "period");
        HashMap<String, Object> sub = new HashMap<>();
        sub.put("propertyName", List.of("start_ts", "end_ts"));
        period.put("subProperties", sub);
        return period;
    }
}
