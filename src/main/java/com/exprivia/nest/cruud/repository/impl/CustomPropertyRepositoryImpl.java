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
 * Custom Property Repository Impl
 */
public class CustomPropertyRepositoryImpl implements CustomPropertyRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Get selected properties
     * @param dto selected properties to find
     * @return all selected properties
     */
    public List<Property> getFilteredProperties(PropertyFilterDto dto) {

        Query query = new Query();

        if(!CollectionUtils.isEmpty(dto.getPropertiesName()))
            query.addCriteria(Criteria.where("name").in(dto.getPropertiesName()));

        if(!CollectionUtils.isEmpty(dto.getIds()))
            query.addCriteria(Criteria.where("id").in(dto.getIds()));

        return mongoTemplate.find(query, Property.class);

    }

}
