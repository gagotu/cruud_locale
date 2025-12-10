package com.exprivia.nest.cruud.utils;

import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import com.exprivia.nest.cruud.mapper.PropertyMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Property Base Test Class
 */
public class PropertyBaseTest {

    protected PropertyDto propertyDto;

    protected static final String ID = UUID.randomUUID().toString();

    @Autowired
    protected PropertyMapper propertyMapper;

    @BeforeEach
    void setUp() {
        HashMap<String, ValueDto> propertyValues = new HashMap<>();
        propertyValues.put("entity_id", ValueDto.builder().name("EnergyConsumerID").build());
        propertyValues.put("state", ValueDto.builder().name("ElectricPower").function("num").alternativeValue("null").build());

        HashMap<String, Object> specificationContextSample = new HashMap<>();
        specificationContextSample.put("name", "User Electric Consumption");
        specificationContextSample.put("version", "2.0");
        specificationContextSample.put("id", Map.of("value", "UserElectricConsumption-2.0", "schemeID", "SCPS"));
        specificationContextSample.put("uri", "https://example.test/ontology/UserElectricConsumption");
        specificationContextSample.put("properties", Map.of(
                "propertyDefinition", List.of(
                        Map.of("propertyName", "EnergyConsumerID", "propertyDescription", "Identificativo utenza energetica", "dataType", "string", "unitOfMeasure", "dimensionless"),
                        Map.of("propertyName", "period", "propertyDescription", "Intervallo di misura",
                                "subProperties", Map.of("propertyName", List.of("start_ts", "end_ts"))),
                        Map.of("propertyName", "ElectricPower", "propertyDescription", "Potenza elettrica", "dataType", "double", "unitOfMeasure", "kilowatt"),
                        Map.of("propertyName", "start_ts", "propertyDescription", "Inizio intervallo", "dataType", "dateTime", "unitOfMeasure", "dimensionless"),
                        Map.of("propertyName", "end_ts", "propertyDescription", "Fine intervallo", "dataType", "dateTime", "unitOfMeasure", "dimensionless")
                )
        ));

        HashMap<String, Object> contextSample = new HashMap<>();
        contextSample.put("coordinates", Map.of("format", "WGS84-DD", "latitude", 0D, "longitude", 0D, "height", 0D));
        contextSample.put("note", "");
        contextSample.put("timestamp", "2025-01-01T00:00:00");
        contextSample.put("timeZone", "UTC+1");
        contextSample.put("language", "IT");
        contextSample.put("producer", Map.of("id", "Solution-ID", "schemeID", "SCPS"));

        HashMap<String, Object> configurations = new HashMap<>();
        configurations.put("date", "last_changed");
        configurations.put("period", List.of("last_changed"));
        configurations.put("slice", 0);
        configurations.put("nullsField", Map.of("ElectricPower", "null"));

        propertyDto = PropertyDto.builder()
                .id(ID)
                .name("lab_load")
                .specification(specificationContextSample)
                .context(contextSample)
                .description("lab load property")
                .createdAt(java.util.Date.from(Instant.parse("2025-01-01T00:00:00Z")))
                .mappings(propertyValues)
                .configurations(configurations)
                .build();
    }

}
