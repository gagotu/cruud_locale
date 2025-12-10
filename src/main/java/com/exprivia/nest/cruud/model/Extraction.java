package com.exprivia.nest.cruud.model;

import com.exprivia.nest.cruud.dto.urbandataset.context.CoordinatesDto;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Extraction Event model
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

    /**
     * Time zone of the source CSV/JSON in IANA format (e.g. "Europe/Rome", "America/New_York").
     * Used to interpret local timestamps when they do not contain an explicit offset.
     */
    private String timeZone;

    /**
     * Fixed UTC offset for the target UrbanDataset in the form "+1", "-2" or "+01:30".
     * All timestamps in the resulting UD will be converted to this offset regardless of the
     * timezone of the input CSV/JSON.
     */
    private String udUtc;

    /**
     * When true the source file does NOT expose an explicit UTC reference and the ingestion
     * must rely on {@link #timeZone} plus DST scrubbing to remove ambiguous timestamps. When
     * false the source already carries an offset/zone and is converted directly.
     */
    @Builder.Default
    private Boolean handle = Boolean.FALSE;

    @Builder.Default
    private Date createdAt = new Date();

}
