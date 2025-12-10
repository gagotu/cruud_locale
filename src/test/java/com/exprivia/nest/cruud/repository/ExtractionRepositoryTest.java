package com.exprivia.nest.cruud.repository;

import com.exprivia.nest.cruud.mapper.ExtractionMapperImpl;
import com.exprivia.nest.cruud.model.Extraction;
import com.exprivia.nest.cruud.repository.impl.CustomExtractionRepositoryImpl;
import com.exprivia.nest.cruud.utils.ExtractionBaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Extraction Repository Test Class
 */
@ExtendWith(SpringExtension.class)
@Import({ExtractionMapperImpl.class, CustomExtractionRepositoryImpl.class})
public class ExtractionRepositoryTest extends ExtractionBaseTest {

    @MockitoBean
    private MongoTemplate mongoTemplate;

    @Autowired
    private CustomExtractionRepositoryImpl extractionRepository;

    @BeforeEach
    void setUpExtractionTest() {
        Mockito.when(mongoTemplate.findOne(Mockito.any(), Mockito.any())).thenReturn(extractionMapper.dtoToModel(extractionDto));
    }

    @Test
    void testFindByName() {
        Date date = new Date();

        Optional<Extraction> extraction = extractionRepository.getByExtractionName("test");
        extraction.get().setCreatedAt(date);

        Optional<Extraction> testExtraction = Optional.ofNullable(extractionMapper.dtoToModel(extractionDto));
        testExtraction.get().setCreatedAt(date);

        assertEquals(extraction, testExtraction);
    }

}
