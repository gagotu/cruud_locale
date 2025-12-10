package com.exprivia.nest.cruud.mapper;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.model.Extraction;
import com.exprivia.nest.cruud.utils.ExtractionBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Extraction Mapper Test
 */
@ExtendWith(SpringExtension.class)
@Import({ExtractionMapperImpl.class})
public class ExtractionMapperTest extends ExtractionBaseTest {

    @Test
    void testDtoToModel() {
        Extraction result = extractionMapper.dtoToModel(extractionDto);
        result.setCreatedAt(extractionDto.getCreatedAt());

        assertEquals(extractionMapper.modelToDto(result), extractionDto);
    }

    @Test
    void testModelToDto() {
        Extraction test = Extraction.builder()
                .id(extractionDto.getId())
                .extractionName(extractionDto.getExtractionName())
                .sourceFilesPath(extractionDto.getSourceFilesPath())
                .outputFilesPath(extractionDto.getOutputFilesPath())
                .sourceRestApi(extractionDto.getSourceRestApi())
                .propertyName(extractionDto.getPropertyName())
                .createdAt(extractionDto.getCreatedAt())
                .separator(extractionDto.getSeparator())
                .autoClean(extractionDto.getAutoClean())
                .autoConvert(extractionDto.getAutoConvert())
                .timeZone(extractionDto.getTimeZone())
                .udUtc(extractionDto.getUdUtc())
                .handle(extractionDto.getHandle())
                .build();

        ExtractionDto result = extractionMapper.modelToDto(test);
        result.setCreatedAt(extractionDto.getCreatedAt());

        assertEquals(result, extractionDto);
    }

}
