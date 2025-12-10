package com.exprivia.nest.cruud.model;

import com.exprivia.nest.cruud.mapper.ExtractionMapperImpl;
import com.exprivia.nest.cruud.utils.ExtractionBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Extraction Test model
 */
@ExtendWith(SpringExtension.class)
@Import({ExtractionMapperImpl.class})
public class ExtractionTest extends ExtractionBaseTest {

    @Test
    void testCreationObject() {
        Extraction extraction = Extraction.builder()
                .id(extractionDto.getId())
                .extractionName(extractionDto.getExtractionName())
                .propertyName(extractionDto.getPropertyName())
                .separator(extractionDto.getSeparator())
                .sourceFilesPath(extractionDto.getSourceFilesPath())
                .outputFilesPath(extractionDto.getOutputFilesPath())
                .sourceRestApi(extractionDto.getSourceRestApi())
                .createdAt(extractionDto.getCreatedAt())
                .autoClean(extractionDto.getAutoClean())
                .autoConvert(extractionDto.getAutoConvert())
                .timeZone(extractionDto.getTimeZone())
                .udUtc(extractionDto.getUdUtc())
                .handle(extractionDto.getHandle())
                .build();

        assertEquals(extractionMapper.modelToDto(extraction), extractionDto);
    }

}
