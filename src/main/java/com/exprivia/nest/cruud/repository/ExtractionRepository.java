package com.exprivia.nest.cruud.repository;

import com.exprivia.nest.cruud.model.Extraction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository Mongo per le estrazioni; estende il custom repo per query dedicate.
 */
@Repository
public interface ExtractionRepository extends MongoRepository<Extraction, String>, CustomExtractionRepository {
}
