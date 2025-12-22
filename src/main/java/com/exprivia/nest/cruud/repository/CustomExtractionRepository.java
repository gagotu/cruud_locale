package com.exprivia.nest.cruud.repository;


import com.exprivia.nest.cruud.model.Extraction;

import java.util.List;
import java.util.Optional;

/**
 * Repository custom per query non standard sulle estrazioni (per nome univoco
 * e per nome property).
 */
public interface CustomExtractionRepository {

    Optional<Extraction> getByExtractionName(String name);

    List<Extraction> findByPropertyName(String name);

}
