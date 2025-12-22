package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.dto.*;
import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.ResultValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.PropertyValueDto;
import com.exprivia.nest.cruud.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@Import({TransformerService.class, ObjectMapper.class, TimeNormalizationService.class})
@TestPropertySource(locations = "classpath:application-test.properties")
class TransformerTimeCasesTest {

    @Autowired
    private TransformerService transformerService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PropertyService propertyService;

    @MockitoBean
    private ExtractionService extractionService;

    @TempDir
    Path tempDir;

    private Path sourceDir;
    private Path outputDir;

    @BeforeEach
    void setUp() throws IOException {
        sourceDir = Files.createDirectories(tempDir.resolve("source"));
        outputDir = Files.createDirectories(tempDir.resolve("output"));
    }

    @Test
    void case1_singleTimestampColumn_isNormalizedToUdUtc() throws Exception {
        Path csv = copyResource("timecases/case1.csv", sourceDir.resolve("case1.csv"));

        PropertyDto propertyDto = baseProperty("lab_load_case1");
        ExtractionDto extraction = baseExtraction("lab_load_case1", csv.getParent().toString(), outputDir.toString(), ',', false);

        Mockito.when(propertyService.getFilteredProperties(Mockito.any(PropertyFilterDto.class)))
                .thenReturn(List.of(propertyDto));
        Mockito.when(extractionService.getByExtractionName(Mockito.anyString()))
                .thenReturn(Optional.of(extraction));

        transformerService.executeConversionFromFolder(extraction);

        ResultUrbanDataset ud = readFirstUd(outputDir);
        assertNotNull(ud);
        List<ResultValueDto> lines = ud.getUrbanDataset().getValues().getLine();
        assertEquals(5, lines.size());

        // Verify first two timestamps and value conversion
        assertEquals(expectedUtc("+2", "2023-10-23T08:19:23.532Z"), lines.get(0).getPeriod().get("start_ts"));
        assertEquals(lines.get(0).getPeriod().get("start_ts"), lines.get(0).getPeriod().get("end_ts"));
        assertEquals("0.78", getPropertyVal(lines.get(0), "ElectricPower"));

        assertEquals(expectedUtc("+2", "2023-10-23T08:19:53.282Z"), lines.get(1).getPeriod().get("start_ts"));
        assertEquals("0.76", getPropertyVal(lines.get(1), "ElectricPower"));

        assertEquals("+2", ud.getUrbanDataset().getContext().getTimeZone());
    }

    @Test
    void case2_separateDateAndTime_usesUtcFromTimeField() throws Exception {
        Path csv = copyResource("timecases/case2.csv", sourceDir.resolve("case2.csv"));

        PropertyDto propertyDto = baseProperty("lab_load_case2");
        // date column plus time column with Z: treat as absolute time
        propertyDto.getConfigurations().put("date", "date");
        propertyDto.getConfigurations().put("period", List.of("period"));

        ExtractionDto extraction = baseExtraction("lab_load_case2", csv.getParent().toString(), outputDir.toString(), ',', false);

        Mockito.when(propertyService.getFilteredProperties(Mockito.any(PropertyFilterDto.class)))
                .thenReturn(List.of(propertyDto));
        Mockito.when(extractionService.getByExtractionName(Mockito.anyString()))
                .thenReturn(Optional.of(extraction));

        transformerService.executeConversionFromFolder(extraction);

        ResultUrbanDataset ud = readFirstUd(outputDir);
        List<ResultValueDto> lines = ud.getUrbanDataset().getValues().getLine();
        assertEquals(5, lines.size());

        assertEquals(expectedUtc("+2", "2023-10-23T08:19:23.532Z"), lines.get(0).getPeriod().get("start_ts"));
        assertEquals(expectedUtc("+2", "2023-10-23T08:23:23.232Z"), lines.get(4).getPeriod().get("start_ts"));
    }

    @Test
    void case3_timeSlots_areExpandedAndConverted() throws Exception {
        Path csv = copyResource("timecases/case3.csv", sourceDir.resolve("case3.csv"));

        PropertyDto propertyDto = propertyForSlots("lab_load_case3");
        ExtractionDto extraction = baseExtraction("lab_load_case3", csv.getParent().toString(), outputDir.toString(), ';', true);

        Mockito.when(propertyService.getFilteredProperties(Mockito.any(PropertyFilterDto.class)))
                .thenReturn(List.of(propertyDto));
        Mockito.when(extractionService.getByExtractionName(Mockito.anyString()))
                .thenReturn(Optional.of(extraction));

        transformerService.executeConversionFromFolder(extraction);

        ResultUrbanDataset ud = readFirstUd(outputDir);
        List<ResultValueDto> lines = ud.getUrbanDataset().getValues().getLine();
        assertEquals(8, lines.size()); // 2 days * 4 intervals

        // First interval of first day (picks value from the corresponding slot)
        assertEquals("2025-04-01T00:00:00", lines.get(0).getPeriod().get("start_ts"));
        assertEquals("2025-04-01T00:15:00", lines.get(0).getPeriod().get("end_ts"));
        assertEquals("0.10", getPropertyVal(lines.get(0), "ElectricPower"));

        // Last interval of second day (all slots carry the last value 0.80 in current mapping)
        ResultValueDto last = lines.get(lines.size() - 1);
        assertEquals("2025-04-02T00:45:00", last.getPeriod().get("start_ts"));
        assertEquals("2025-04-02T01:00:00", last.getPeriod().get("end_ts"));
        assertEquals("0.80", getPropertyVal(last, "ElectricPower"));
    }

    @Test
    void case4_sliceBasedPeriodsNormalizeOnlyStartAndKeepDuration() throws Exception {
        Path csv = copyResource("timecases/case4.csv", sourceDir.resolve("case4.csv"));

        PropertyDto propertyDto = propertyWithSlice("lab_load_case4", 20);
        ExtractionDto extraction = baseExtraction("lab_load_case4", csv.getParent().toString(), outputDir.toString(), ',', true);

        Mockito.when(propertyService.getFilteredProperties(Mockito.any(PropertyFilterDto.class)))
                .thenReturn(List.of(propertyDto));
        Mockito.when(extractionService.getByExtractionName(Mockito.anyString()))
                .thenReturn(Optional.of(extraction));

        transformerService.executeConversionFromFolder(extraction);

        ResultUrbanDataset ud = readFirstUd(outputDir);
        List<ResultValueDto> lines = ud.getUrbanDataset().getValues().getLine();
        assertEquals(4, lines.size());

        for (ResultValueDto line : lines) {
            String startStr = Objects.toString(line.getPeriod().get("start_ts"), null);
            String endStr = Objects.toString(line.getPeriod().get("end_ts"), null);
            assertNotNull(startStr);
            assertNotNull(endStr);
            LocalDateTime start = LocalDateTime.parse(startStr);
            LocalDateTime end = LocalDateTime.parse(endStr);
            assertEquals(Duration.ofMinutes(20), Duration.between(start, end));
        }

        assertEquals("2023-10-29T00:40:00", lines.get(0).getPeriod().get("start_ts"));
        assertEquals("2023-10-29T01:00:00", lines.get(0).getPeriod().get("end_ts"));
        assertEquals("2023-10-29T01:40:00", lines.get(3).getPeriod().get("start_ts"));
        assertEquals("2023-10-29T02:00:00", lines.get(3).getPeriod().get("end_ts"));
    }

    private PropertyDto baseProperty(String name) {
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

        HashMap<String, Object> context = new HashMap<>();
        HashMap<String, Object> producer = new HashMap<>();
        producer.put("id", "Solution-ID");
        producer.put("schemeID", "SCPS");
        context.put("producer", producer);
        context.put("timeZone", "UTC+1");

        HashMap<String, ValueDto> mappings = new HashMap<>();
        mappings.put("entity_id", ValueDto.builder().name("EnergyConsumerID").build());
        mappings.put("state", ValueDto.builder().name("ElectricPower").function("num").alternativeValue("null").build());

        HashMap<String, Object> configurations = new HashMap<>();
        configurations.put("slice", 0);
        configurations.put("period", List.of("last_changed"));
        configurations.put("date", "last_changed");
        HashMap<String, String> nullsField = new HashMap<>();
        nullsField.put("ElectricPower", "null");
        configurations.put("nullsField", nullsField);

        return PropertyDto.builder()
                .name(name)
                .specification(specification)
                .context(context)
                .mappings(mappings)
                .configurations(configurations)
                .build();
    }

    private PropertyDto propertyForSlots(String name) {
        HashMap<String, Object> specification = new HashMap<>();
        HashMap<String, Object> id = new HashMap<>();
        id.put("value", "test-id");
        id.put("schemeID", "SCPS");
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("propertyDefinition", List.of(
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

        HashMap<String, Object> context = new HashMap<>();
        HashMap<String, Object> producer = new HashMap<>();
        producer.put("id", "Solution-ID");
        producer.put("schemeID", "SCPS");
        context.put("producer", producer);
        context.put("timeZone", "UTC+1");

        HashMap<String, ValueDto> mappings = new HashMap<>();
        String[] slots = {"00:00-00:15", "00:15-00:30", "00:30-00:45", "00:45-01:00"};
        for (String slot : slots) {
            mappings.put(slot, ValueDto.builder().name("ElectricPower").function("num").alternativeValue("null").build());
        }

        HashMap<String, Object> configurations = new HashMap<>();
        configurations.put("slice", 0);
        configurations.put("period", java.util.Arrays.asList(slots));
        configurations.put("date", "Giorno");
        HashMap<String, String> nullsField = new HashMap<>();
        nullsField.put("ElectricPower", "null");
        configurations.put("nullsField", nullsField);

        return PropertyDto.builder()
                .name(name)
                .specification(specification)
                .context(context)
                .mappings(mappings)
                .configurations(configurations)
                .build();
    }

    private PropertyDto propertyWithSlice(String name, int sliceMinutes) {
        PropertyDto base = baseProperty(name);
        base.getConfigurations().put("slice", sliceMinutes);
        base.getConfigurations().put("period", List.of("orario"));
        base.getConfigurations().put("date", "data");
        return base;
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

    private ExtractionDto baseExtraction(String name, String source, String output, char separator, boolean handle) {
        return ExtractionDto.builder()
                .extractionName(name)
                .propertyName(name)
                .sourceFilesPath(source)
                .outputFilesPath(output)
                .separator(separator)
                .handle(handle)
                .timeZone("Europe/Rome")
                .udUtc("+2")
                .build();
    }

    private Path copyResource(String resourcePath, Path target) throws IOException {
        try (var in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) throw new IOException("Resource not found: " + resourcePath);
            Files.copy(in, target);
        }
        return target;
    }

    private ResultUrbanDataset readFirstUd(Path outputDir) throws IOException {
        try (var files = Files.list(outputDir)) {
            Path udFile = files.filter(p -> p.toString().endsWith(".json")).findFirst()
                    .orElseThrow(() -> new IOException("No UD file produced"));
            return objectMapper.readValue(udFile.toFile(), ResultUrbanDataset.class);
        }
    }

    private String expectedUtc(String targetOffset, String isoInstantOrOffset) {
        ZoneOffset target = Utils.parseUtcOffset(targetOffset);
        var instant = OffsetDateTime.parse(isoInstantOrOffset).toInstant();
        return instant.atOffset(target).toLocalDateTime().toString();
    }

    private String getPropertyVal(ResultValueDto rv, String name) {
        if (rv.getProperty() == null) return null;
        for (PropertyValueDto pv : rv.getProperty()) {
            if (pv.getName().equalsIgnoreCase(name)) {
                return pv.getVal();
            }
        }
        return null;
    }
}
