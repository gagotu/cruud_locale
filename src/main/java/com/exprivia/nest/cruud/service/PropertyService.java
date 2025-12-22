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
 * Servizio applicativo per gestire le property di mapping: CRUD, filtro dinamico
 * e gestione dell'eccezione per nomi duplicati, con mapping DTO↔modello.
 */
@Slf4j
@Service
public class PropertyService {

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private PropertyMapper propertyMapper;

    /** Crea una property, intercettando l'errore di nome duplicato. */
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

    /** Restituisce tutte le property. */
    public List<PropertyDto> getAll() {
        log.debug("Property Service: getAll");
        return convertListInDto(propertyRepository.findAll());
    }

    /** Restituisce una property per id. */
    public Optional<PropertyDto> getById(String id) {
        log.debug("Property Service: getById -> {}", id);
        return propertyRepository.findById(id).map(propertyMapper::modelToDto);
    }

    /** Cancella una property per id. */
    public void remove(String id) {
        log.debug("Property Service: remove -> {}", id);
        propertyRepository.deleteById(id);
    }

    /** Restituisce le property filtrate secondo i criteri del filtro. */
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
