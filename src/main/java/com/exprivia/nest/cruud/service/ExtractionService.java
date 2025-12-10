package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.mapper.ExtractionMapper;
import com.exprivia.nest.cruud.model.Extraction;
import com.exprivia.nest.cruud.repository.ExtractionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Extraction Service that expose business methods for Extraction Events
 */
@Slf4j
@Service
public class ExtractionService {

    @Autowired
    private ExtractionRepository extractionRepository;

    @Autowired
    private ExtractionMapper extractionMapper;

    /**
     * Method to create Extraction Event
     *
     * @param dto to create
     * @return dto created
     */
    public ExtractionDto create(ExtractionDto dto) {
        log.debug("Extraction Service: Create -> {}", dto);

        return extractionMapper.modelToDto(
                extractionRepository.save(extractionMapper.dtoToModel(dto))
        );
    }

    /**
     * Get all Extraction Events
     *
     * @return ExtractionEvents list
     */
    public List<ExtractionDto> getAll() {
        log.debug("Extraction Service: getAll");

        return convertListInDto(extractionRepository.findAll());
    }

    /**
     * Get optional extraction event by id
     *
     * @param id to search
     * @return optional dto
     */
    public Optional<ExtractionDto> getById(String id) {
        log.debug("Extraction Service: getById -> {}", id);

        return extractionRepository.findById(id).map(extractionMapper::modelToDto);
    }

    /**
     * Get extraction by name method
     *
     * @param extractionName name of extraction
     * @return extraction
     */
    public Optional<ExtractionDto> getByExtractionName(String extractionName) {
        log.debug("Extraction Service: getByName -> {}", extractionName);

        return extractionRepository.getByExtractionName(extractionName).map(extractionMapper::modelToDto);
    }

    /**
     * Get extractions list by name property
     *
     * @param propertyName name of property
     * @return extractions list
     */
    public List<ExtractionDto> findByPropertyName(String propertyName) {
        log.debug("Extraction Service: findByPropertyName -> {}", propertyName);

        return extractionRepository.findByPropertyName(propertyName).stream()
                .map(extractionMapper::modelToDto)
                .toList();
    }

    /**
     * Remove extraction event by id
     *
     * @param id to remove
     */
    public void remove(String id) {
        log.debug("Extraction Service: remove -> {}", id);
        extractionRepository.deleteById(id);
    }

    private List<ExtractionDto> convertListInDto(List<Extraction> extractions) {
        return extractions
                .stream()
                .map(extractionMapper::modelToDto)
                .toList();
    }

}
