package com.exprivia.nest.cruud.repository.impl;

import com.exprivia.nest.cruud.dto.PropertyFilterDto;
import com.exprivia.nest.cruud.model.Property;
import com.exprivia.nest.cruud.repository.CustomPropertyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * Implementazione custom per query dinamiche sulle property. Costruisce filtri
 * su nome e id utilizzando MongoTemplate.
 */
public class CustomPropertyRepositoryImpl implements CustomPropertyRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    /** Restituisce le property filtrate in base ai criteri indicati nel filtro. */
    public List<Property> getFilteredProperties(PropertyFilterDto dto) {

        Query query = new Query();

        if(!CollectionUtils.isEmpty(dto.getPropertiesName()))
            query.addCriteria(Criteria.where("name").in(dto.getPropertiesName()));

        if(!CollectionUtils.isEmpty(dto.getIds()))
            query.addCriteria(Criteria.where("id").in(dto.getIds()));

        return mongoTemplate.find(query, Property.class);

    }

}
