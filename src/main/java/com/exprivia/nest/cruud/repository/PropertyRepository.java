package com.exprivia.nest.cruud.repository;

import com.exprivia.nest.cruud.model.Property;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Property Repository
 */
@Repository
public interface PropertyRepository extends MongoRepository<Property, String>, CustomPropertyRepository {
}
