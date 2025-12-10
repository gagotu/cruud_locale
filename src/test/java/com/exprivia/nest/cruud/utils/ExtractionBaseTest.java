package com.exprivia.nest.cruud.utils;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.mapper.ExtractionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Extraction Base Test
 */
public class ExtractionBaseTest {

    protected ExtractionDto extractionDto;

    protected static final String ID = UUID.randomUUID().toString();

    @Autowired
    protected ExtractionMapper extractionMapper;

    @BeforeEach
    void setUp() {
        extractionDto = ExtractionDto.builder()
                .id(ID)
                .extractionName("lab_load")
                .sourceFilesPath(Path.of("src", "test", "resources", "source").toString())
                .outputFilesPath(Path.of("src", "test", "resources", "output").toString())
                .sourceRestApi("http://localhost:8080/test_api")
                .propertyName("lab_load")
                .separator(',')
                .autoClean(Boolean.TRUE)
                .autoConvert(Boolean.FALSE)
                .timeZone("Europe/Rome")
                .udUtc("+2")
                .handle(Boolean.FALSE)
                .build();
    }

}
