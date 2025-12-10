package com.exprivia.nest.cruud.repository.impl;

import com.exprivia.nest.cruud.model.Extraction;
import com.exprivia.nest.cruud.repository.CustomExtractionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Optional;

public class CustomExtractionRepositoryImpl implements CustomExtractionRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Repository method to get extraction by name
     *
     * @param name of extraction
     * @return extraction
     */
    public Optional<Extraction> getByExtractionName(String name) {

        Query query = new Query();

        if (!name.isBlank())
            query.addCriteria(Criteria.where("extractionName").is(name));

        return Optional.ofNullable(mongoTemplate.findOne(query, Extraction.class));
    }

    /**
     * Repository method to retrieve a list of extractions for property searched
     *
     * @param name of property
     * @return extraction list
     */
    public List<Extraction> findByPropertyName(String name) {

        Query query = new Query();

        if (!name.isBlank())
            query.addCriteria(Criteria.where("propertyName").is(name));

        return mongoTemplate.find(query, Extraction.class);

    }

}
