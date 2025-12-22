package com.exprivia.nest.cruud.repository.impl;

import com.exprivia.nest.cruud.model.Extraction;
import com.exprivia.nest.cruud.repository.CustomExtractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

/**
 * Implementazione custom per query su Extraction non coperte dal repository standard.
 * Usa MongoTemplate per costruire filtri dinamici per nome e propertyName.
 */
public class CustomExtractionRepositoryImpl implements CustomExtractionRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    /** Restituisce l'estrazione con un dato nome (univoco). */
    public Optional<Extraction> getByExtractionName(String name) {

        Query query = new Query();

        if (!name.isBlank())
            query.addCriteria(Criteria.where("extractionName").is(name));

        return Optional.ofNullable(mongoTemplate.findOne(query, Extraction.class));
    }

    /** Restituisce le estrazioni legate a una determinata property. */
    public List<Extraction> findByPropertyName(String name) {

        Query query = new Query();

        if (!name.isBlank())
            query.addCriteria(Criteria.where("propertyName").is(name));

        return mongoTemplate.find(query, Extraction.class);

    }

}
