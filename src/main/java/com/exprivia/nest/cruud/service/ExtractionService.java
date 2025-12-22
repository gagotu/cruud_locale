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
 * Servizio applicativo per gestire le configurazioni di estrazione: CRUD e
 * ricerca per nome/property, con mapping tra DTO e modello Mongo.
 */
@Slf4j
@Service
public class ExtractionService {

    @Autowired
    private ExtractionRepository extractionRepository;

    @Autowired
    private ExtractionMapper extractionMapper;

    /** Crea una nuova estrazione e restituisce il DTO persistito. */
    public ExtractionDto create(ExtractionDto dto) {
        log.debug("Extraction Service: Create -> {}", dto);

        return extractionMapper.modelToDto(
                extractionRepository.save(extractionMapper.dtoToModel(dto))
        );
    }

    /** Restituisce tutte le estrazioni. */
    public List<ExtractionDto> getAll() {
        log.debug("Extraction Service: getAll");

        return convertListInDto(extractionRepository.findAll());
    }

    /** Restituisce l'estrazione per id (se esiste). */
    public Optional<ExtractionDto> getById(String id) {
        log.debug("Extraction Service: getById -> {}", id);

        return extractionRepository.findById(id).map(extractionMapper::modelToDto);
    }

    /** Restituisce l'estrazione per nome univoco. */
    public Optional<ExtractionDto> getByExtractionName(String extractionName) {
        log.debug("Extraction Service: getByName -> {}", extractionName);

        return extractionRepository.getByExtractionName(extractionName).map(extractionMapper::modelToDto);
    }

    /** Restituisce le estrazioni collegate a una property. */
    public List<ExtractionDto> findByPropertyName(String propertyName) {
        log.debug("Extraction Service: findByPropertyName -> {}", propertyName);

        return extractionRepository.findByPropertyName(propertyName).stream()
                .map(extractionMapper::modelToDto)
                .toList();
    }

    /** Cancella una estrazione per id. */
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
