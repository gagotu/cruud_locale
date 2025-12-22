package com.exprivia.nest.cruud.model;

import com.exprivia.nest.cruud.dto.urbandataset.context.CoordinatesDto;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Documento Mongo che rappresenta una configurazione di estrazione:
 * nome univoco, property associata, sorgenti e parametri di gestione fusi.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document("Extractions")
public class Extraction {

    @Id
    private String id;

    @NonNull
    @Indexed(unique = true)
    private String extractionName;

    @NonNull
    private String propertyName;

    private String sourceRestApi;

    private String sourceFilesPath;

    private String outputFilesPath;

    private char separator;

    private String producerID;

    private CoordinatesDto coordinates;

    @Builder.Default
    private Boolean autoClean = Boolean.FALSE;

    @Builder.Default
    private Boolean autoConvert = Boolean.FALSE;

    /** Fuso della sorgente per interpretare timestamp privi di offset. */
    private String timeZone;

    /** Offset di destinazione dell'UrbanDataset. */
    private String udUtc;

    /** Flag per abilitare la gestione manuale degli orari senza offset esplicito. */
    @Builder.Default
    private Boolean handle = Boolean.FALSE;

    /** Flag per abilitare la logica di merge sulle fasce orarie giornaliere. */
    @Builder.Default
    private Boolean fasce = Boolean.FALSE;

    @Builder.Default
    private Date createdAt = new Date();

}
