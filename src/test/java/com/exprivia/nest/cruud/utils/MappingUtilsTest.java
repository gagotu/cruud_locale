package com.exprivia.nest.cruud.utils;

import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.PropertyValueDto;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingUtilsTest {

    @Test
    void addNullFieldsAddsMissingKeys() {
        List<PropertyValueDto> values = new ArrayList<>();
        values.add(PropertyValueDto.builder().name("Energy").val("1").build());

        Map<String, String> nulls = new HashMap<>();
        nulls.put("Voltage", "null");

        MappingUtils.addNullFields(values, nulls);

        assertTrue(values.stream().anyMatch(item -> "Voltage".equalsIgnoreCase(item.getName()) && "null".equals(item.getVal())));
    }

    @Test
    void getCorrectValueUsesDictionaryFunction() {
        List<ValueDto> dictionary = List.of(ValueDto.builder()
                .name("Power")
                .function("num")
                .alternativeValue("null")
                .build());

        assertEquals("10.5", MappingUtils.getCorrectValue("Power", "10,5", dictionary));
    }
}
