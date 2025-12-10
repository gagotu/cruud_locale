package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.dto.PropertyDto;
import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.exception.DuplicateNameException;
import com.exprivia.nest.cruud.exception.ExceptionMessage;
import com.exprivia.nest.cruud.mapper.PropertyMapper;
import com.exprivia.nest.cruud.model.Property;
import com.exprivia.nest.cruud.repository.PropertyRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Property Service that expose business methods for Properties
 */
@Slf4j
@Service
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyMapper propertyMapper;

    /**
     * Method to create a property
     * @param dto to create
     * @return property created
     * @throws DuplicateNameException exception for duplicate properties name into DB
     */
    public PropertyDto create(PropertyDto dto) throws DuplicateNameException {
        log.debug("Property Service: Create -> {}", dto);

        try {
            return propertyMapper.modelToDto(
                    propertyRepository.save(propertyMapper.dtoToModel(dto))
            );
        } catch (DuplicateKeyException e) {
            throw new DuplicateNameException(ExceptionMessage.DUPLICATE_NAME_EXCEPTION);
        }

    }

    /**
     * Method to retrieve all properties
     * @return all properties
     */
    public List<PropertyDto> getAll() {
        log.debug("Property Service: getAll");
        return convertListInDto(propertyRepository.findAll());
    }

    /**
     * Method to retrieve a property by id
     * @param id to find
     * @return property found
     */
    public Optional<PropertyDto> getById(String id) {
        log.debug("Property Service: getById -> {}", id);
        return propertyRepository.findById(id).map(propertyMapper::modelToDto);
    }

    /**
     * Method to remove property by id
     * @param id to remove property
     */
    public void remove(String id) {
        log.debug("Property Service: remove -> {}", id);
        propertyRepository.deleteById(id);
    }

    /**
     * Method to retrieve filtered properties
     * @param dto to retrieve filtered properties
     * @return filtered properties
     */
    public List<PropertyDto> getFilteredProperties(PropertyFilterDto dto) {
        log.debug("Property Service: getFilteredProperties -> {}", dto);
        return convertListInDto(propertyRepository.getFilteredProperties(dto));
    }

    private List<PropertyDto> convertListInDto(List<Property> properties) {
        return properties
                .stream()
                .map(propertyMapper::modelToDto)
                .toList();
    }

}
