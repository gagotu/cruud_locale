package com.exprivia.nest.cruud.controller;

import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.exception.DuplicateNameException;
import com.exprivia.nest.cruud.service.PropertyService;
import com.exprivia.nest.cruud.utils.Endpoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * Property Controller that expose REST API for mappings and configurations
 */
@Slf4j
@RestController
@RequestMapping(Endpoints.PROPERTY)
public class PropertyController {

    @Autowired
    private PropertyService propertyService;

    /**
     * Method to create a property
     * @param dto param
     * @return dto created
     * @throws DuplicateNameException exception for duplicate properties name into DB
     */
    @PostMapping
    public PropertyDto create(@RequestBody PropertyDto dto) throws DuplicateNameException {
        log.debug("Property Controller: Create -> {}", dto);
        return propertyService.create(dto);
    }

    /**
     * Method to retrieve all properties
     * @return all properties
     */
    @GetMapping(Endpoints.ALL)
    public List<PropertyDto> getAll() {
        log.debug("Property Controller: getAll");
        return propertyService.getAll();
    }

    /**
     * Method to return a searched property (by id)
     * @param id string to search
     * @return property found
     */
    @GetMapping(Endpoints.SLASH + "{id}")
    public Optional<PropertyDto> getById(@PathVariable String id) {
        log.debug("Property Controller: getById -> {}", id);
        return propertyService.getById(id);
    }

    /**
     * Method to remove property by id
     * @param id string to delete
     */
    @DeleteMapping(Endpoints.DELETE + Endpoints.SLASH + "{id}")
    public void remove(@PathVariable String id) {
        log.debug("Property Controller: remove -> {}", id);
        propertyService.remove(id);
    }

    /**
     * Method to retrieve filtered properties by name
     * @param dto that contain filtered properties by name
     * @return filtered properties
     */
    @PostMapping(Endpoints.FILTER)
    public List<PropertyDto> getFilteredProperties(@RequestBody PropertyFilterDto dto) {
        log.debug("Property Controller: getFilteredProperties -> {}", dto);
        return propertyService.getFilteredProperties(dto);
    }

}
