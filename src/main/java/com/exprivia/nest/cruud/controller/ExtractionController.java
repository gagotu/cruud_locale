package com.exprivia.nest.cruud.controller;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.service.ExtractionService;
import com.exprivia.nest.cruud.utils.Endpoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Extraction controller that expose REST API for mappings and configurations
 */
@Slf4j
@RestController
@RequestMapping(Endpoints.EXTRACTION)
public class ExtractionController {

    @Autowired
    private ExtractionService extractionService;

    /**
     * Create Extraction document into DB
     *
     * @param dto param
     * @return dto created
     */
    @PostMapping
    public ExtractionDto create(@RequestBody ExtractionDto dto) {
        log.debug("Extraction Controller: Create -> {}", dto);
        return extractionService.create(dto);
    }

    /**
     * Retrieve all extractions from DB
     *
     * @return all dtos
     */
    @GetMapping(Endpoints.ALL)
    public List<ExtractionDto> getAll() {
        log.debug("Extraction Controller: getAll");
        return extractionService.getAll();
    }

    /**
     * Retrieve extraction from DB
     *
     * @param id of extraction to retrieve
     * @return dto
     */
    @GetMapping(Endpoints.SLASH + "{id}")
    public Optional<ExtractionDto> getById(@PathVariable String id) {
        log.debug("Extraction Controller: getById -> {}", id);
        return extractionService.getById(id);
    }

    /**
     * Retrieve extraction by name
     *
     * @param extractionName name of extraction
     * @return extraction
     */
    @GetMapping(Endpoints.NAME + Endpoints.SLASH + "{extractionName}")
    public Optional<ExtractionDto> getByExtractionName(@PathVariable String extractionName) {
        log.debug("Extraction Controller: getByExtractionName -> {}", extractionName);
        return extractionService.getByExtractionName(extractionName);
    }

    /**
     * Retrieve extractions list by property name
     *
     * @param propertyName property name
     * @return extractions list
     */
    @GetMapping(Endpoints.PROPERTY + Endpoints.SLASH + "{propertyName}")
    public List<ExtractionDto> findByPropertyName(@PathVariable String propertyName) {
        log.debug("Extraction Controller: getByPropertyName -> {}", propertyName);
        return extractionService.findByPropertyName(propertyName);
    }

    /**
     * Remove extraction by id
     *
     * @param id to remove
     */
    @DeleteMapping(Endpoints.DELETE + Endpoints.SLASH + "{id}")
    public void remove(@PathVariable String id) {
        log.debug("Property Controller: remove -> {}", id);
        extractionService.remove(id);
    }

}
