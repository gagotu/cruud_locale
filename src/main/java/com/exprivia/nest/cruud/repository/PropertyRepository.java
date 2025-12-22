package com.exprivia.nest.cruud.repository;

import com.exprivia.nest.cruud.model.Property;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository Mongo per le property; estende il custom repo per filtri dinamici.
 */
@Repository
public interface PropertyRepository extends MongoRepository<Property, String>, CustomPropertyRepository {
}
