package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.dto.*;
import com.exprivia.nest.cruud.dto.sourcedataset.DynamicValueDto;
import com.exprivia.nest.cruud.dto.sourcedataset.MetadataDto;
import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import com.exprivia.nest.cruud.mapper.ExtractionMapperImpl;
import com.exprivia.nest.cruud.utils.ExtractionBaseTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(SpringExtension.class)
@Import({ExternalService.class, ObjectMapper.class, ExtractionMapperImpl.class})
@TestPropertySource(locations = "classpath:application-test.properties")
public class ExternalServiceTest extends ExtractionBaseTest {

    @Autowired
    private ExternalService externalService;

    @MockitoBean
    private PropertyService propertyService;

    @MockitoBean
    private ExtractionService extractionService;

    private JSONDto jsonDto;

    @BeforeEach
    void setUp() {
        DynamicValueDto dynamicDto = new DynamicValueDto();
        dynamicDto.getAttributes().put("entity_id", "sensor.lab_active_power");
        dynamicDto.getAttributes().put("state", 0.85);
        dynamicDto.getAttributes().put("last_changed", "2023-10-23T08:25:23.292Z");

        jsonDto = JSONDto.builder()
                .metadata(List.of(MetadataDto.builder()
                        .name("name")
                        .description("description")
                        .type("type")
                        .example("example")
                        .unitOfMeasure("unitOfMeasure").build()))
                .result(List.of(dynamicDto))
                .build();
    }

    @Test
    void testExecuteConversionFromOpenCruise() {
        PropertyDto property = buildPropertyForExternal();

        Mockito.when(propertyService.getFilteredProperties(Mockito.any(PropertyFilterDto.class)))
                .thenReturn(List.of(property));

        Mockito.when(extractionService.getByExtractionName(Mockito.anyString())).thenReturn(
                Optional.ofNullable(extractionDto)
        );

        ResultUrbanDataset resultUrbanDataset = externalService.executeConversionFromOpenCruise(jsonDto, "property");

        assertNotNull(resultUrbanDataset);
    }

    private PropertyDto buildPropertyForExternal() {
        HashMap<String, ValueDto> mappings = new HashMap<>();
        mappings.put("entity_id", ValueDto.builder().name("EnergyConsumerID").build());
        mappings.put("state", ValueDto.builder().name("ElectricPower").function("num").alternativeValue("null").build());

        HashMap<String, Object> specification = new HashMap<>();
        specification.put("name", "User Electric Consumption");
        specification.put("version", "2.0");
        specification.put("id", java.util.Map.of("value", "UserElectricConsumption-2.0", "schemeID", "SCPS"));
        specification.put("uri", "https://example.test/ontology/UserElectricConsumption");
        specification.put("properties", java.util.Map.of(
                "propertyDefinition", List.of(
                        java.util.Map.of("propertyName", "EnergyConsumerID", "propertyDescription", "Identificativo utenza energetica", "dataType", "string", "unitOfMeasure", "dimensionless"),
                        java.util.Map.of("propertyName", "period", "propertyDescription", "Intervallo di misura",
                                "subProperties", java.util.Map.of("propertyName", List.of("start_ts", "end_ts"))),
                        java.util.Map.of("propertyName", "ElectricPower", "propertyDescription", "Potenza elettrica", "dataType", "double", "unitOfMeasure", "kilowatt"),
                        java.util.Map.of("propertyName", "start_ts", "propertyDescription", "Inizio intervallo", "dataType", "dateTime", "unitOfMeasure", "dimensionless"),
                        java.util.Map.of("propertyName", "end_ts", "propertyDescription", "Fine intervallo", "dataType", "dateTime", "unitOfMeasure", "dimensionless")
                )
        ));

        HashMap<String, Object> context = new HashMap<>();
        context.put("coordinates", java.util.Map.of("format", "WGS84-DD", "latitude", 0D, "longitude", 0D, "height", 0D));
        context.put("note", "");
        context.put("timestamp", "2025-01-01T00:00:00");
        context.put("timeZone", "UTC+1");
        context.put("language", "IT");
        context.put("producer", java.util.Map.of("id", "Solution-ID", "schemeID", "SCPS"));

        HashMap<String, Object> configurations = new HashMap<>();
        configurations.put("date", "last_changed");
        configurations.put("period", List.of("last_changed"));
        configurations.put("slice", 0);
        configurations.put("nullsField", java.util.Map.of("ElectricPower", "null"));

        return PropertyDto.builder()
                .id("id")
                .name("lab_load")
                .description("lab load property")
                .mappings(mappings)
                .specification(specification)
                .context(context)
                .configurations(configurations)
                .build();
    }

}
