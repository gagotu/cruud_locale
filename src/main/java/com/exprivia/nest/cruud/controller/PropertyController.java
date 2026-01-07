package com.exprivia.nest.cruud.controller;

import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.exception.DuplicateNameException;
import com.exprivia.nest.cruud.exception.NotFoundException;
import com.exprivia.nest.cruud.service.PropertyService;
import com.exprivia.nest.cruud.utils.Endpoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Espone le API REST per gestire le proprietà (mapping UD) usate durante le conversioni.
 */
@Slf4j
@RestController
@RequestMapping(Endpoints.PROPERTY)
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    /** Crea una property; solleva DuplicateNameException se esiste già. */
    @PostMapping
    public PropertyDto create(@RequestBody PropertyDto dto) throws DuplicateNameException {
        log.debug("Property Controller: Create -> {}", dto);
        return propertyService.create(dto);
    }

    /** Restituisce tutte le property configurate. */
    @GetMapping(Endpoints.ALL)
    public List<PropertyDto> getAll() {
        log.debug("Property Controller: getAll");
        return propertyService.getAll();
    }

    /** Restituisce una property cercandola per id. */
    @GetMapping(Endpoints.SLASH + "{id}")
    public ResponseEntity<PropertyDto> getById(@PathVariable String id) {
        log.debug("Property Controller: getById -> {}", id);
        return propertyService.getById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Property not found for id " + id));
    }

    /** Cancella una property per id. */
    @DeleteMapping(Endpoints.DELETE + Endpoints.SLASH + "{id}")
    public void remove(@PathVariable String id) {
        log.debug("Property Controller: remove -> {}", id);
        propertyService.remove(id);
    }

    /** Restituisce le property filtrate per nome (payload nel dto). */
    @PostMapping(Endpoints.FILTER)
    public List<PropertyDto> getFilteredProperties(@RequestBody PropertyFilterDto dto) {
        log.debug("Property Controller: getFilteredProperties -> {}", dto);
        return propertyService.getFilteredProperties(dto);
    }

}
