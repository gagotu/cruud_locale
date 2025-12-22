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
 * Espone le API REST per gestire le configurazioni di estrazione (crea, legge,
 * cancella e ricerca per nome/proprietà).
 */
@Slf4j
@RestController
@RequestMapping(Endpoints.EXTRACTION)
public class ExtractionController {

    @Autowired
    private ExtractionService extractionService;

    /** Crea una nuova configurazione di estrazione in DB. */
    @PostMapping
    public ExtractionDto create(@RequestBody ExtractionDto dto) {
        log.debug("Extraction Controller: Create -> {}", dto);
        return extractionService.create(dto);
    }

    /** Recupera tutte le estrazioni configurate. */
    @GetMapping(Endpoints.ALL)
    public List<ExtractionDto> getAll() {
        log.debug("Extraction Controller: getAll");
        return extractionService.getAll();
    }

    /** Recupera una estrazione specifica tramite id. */
    @GetMapping(Endpoints.SLASH + "{id}")
    public Optional<ExtractionDto> getById(@PathVariable String id) {
        log.debug("Extraction Controller: getById -> {}", id);
        return extractionService.getById(id);
    }

    /** Recupera una estrazione tramite il nome univoco. */
    @GetMapping(Endpoints.NAME + Endpoints.SLASH + "{extractionName}")
    public Optional<ExtractionDto> getByExtractionName(@PathVariable String extractionName) {
        log.debug("Extraction Controller: getByExtractionName -> {}", extractionName);
        return extractionService.getByExtractionName(extractionName);
    }

    /** Recupera le estrazioni che fanno riferimento a una certa property. */
    @GetMapping(Endpoints.PROPERTY + Endpoints.SLASH + "{propertyName}")
    public List<ExtractionDto> findByPropertyName(@PathVariable String propertyName) {
        log.debug("Extraction Controller: getByPropertyName -> {}", propertyName);
        return extractionService.findByPropertyName(propertyName);
    }

    /** Rimuove una estrazione dal DB. */
    @DeleteMapping(Endpoints.DELETE + Endpoints.SLASH + "{id}")
    public void remove(@PathVariable String id) {
        log.debug("Property Controller: remove -> {}", id);
        extractionService.remove(id);
    }

}
