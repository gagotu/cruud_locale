package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.mapper.ExtractionMapperImpl;
import com.exprivia.nest.cruud.model.Extraction;
import com.exprivia.nest.cruud.repository.ExtractionRepository;
import com.exprivia.nest.cruud.utils.ExtractionBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Extraction Service Test
 */
@ExtendWith(SpringExtension.class)
@Import({ExtractionMapperImpl.class, ExtractionService.class})
public class ExtractionServiceTest extends ExtractionBaseTest {

    @MockitoBean
    private ExtractionRepository extractionRepository;

    @Autowired
    private ExtractionService extractionService;

    @Test
    void createTest() {
        Mockito.when(extractionRepository.save(Mockito.any(Extraction.class))).thenReturn(extractionMapper.dtoToModel(extractionDto));

        var create = extractionService.create(extractionDto);
        create.setCreatedAt(extractionDto.getCreatedAt());

        assertEquals(create, extractionDto);
    }

    @Test
    void getAllTest() {
        Mockito.when(extractionRepository.findAll()).thenReturn(List.of(extractionMapper.dtoToModel(extractionDto)));

        var result = extractionService.getAll();
        result = result.stream().peek(dto -> dto.setCreatedAt(extractionDto.getCreatedAt())).toList();

        assertEquals(List.of(extractionDto), result);
    }

    @Test
    void getExtractionByIdTest() {
        Mockito.when(extractionRepository.findById(Mockito.anyString())).thenReturn(Optional.ofNullable(extractionMapper.dtoToModel(extractionDto)));

        var result = extractionService.getById(ID);

        result.ifPresent( dto -> {
            dto.setCreatedAt(extractionDto.getCreatedAt());
            assertEquals(extractionDto, dto);
        });
    }

    @Test
    void removePropertyById() {
        Mockito.doNothing().when(extractionRepository).deleteById(Mockito.anyString());
        extractionService.remove(ID);
    }

}
