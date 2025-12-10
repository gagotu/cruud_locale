package com.exprivia.nest.cruud.repository;


import com.exprivia.nest.cruud.model.Extraction;

import java.util.List;
import java.util.Optional;

/**
 * Custom extraction repository interface
 */
public interface CustomExtractionRepository {

    Optional<Extraction> getByExtractionName(String name);

    List<Extraction> findByPropertyName(String name);

}
