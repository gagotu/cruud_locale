package com.exprivia.nest.cruud.dto;

import com.exprivia.nest.cruud.dto.urbandataset.context.CoordinatesDto;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import java.util.Date;

/**
 * Class Extraction
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionDto {

    private String id;

    @NonNull
    private String extractionName;

    @NonNull
    private String propertyName;

    private String sourceRestApi;

    private String sourceFilesPath;

    private String outputFilesPath;

    private char separator;

    private String producerID;

    private CoordinatesDto coordinates;

    private Boolean autoClean;

    private Boolean autoConvert;

    private Date createdAt;

    /**
     * Time zone of the source CSV/JSON in IANA format (e.g. "Europe/Rome", "America/New_York").
     * Used to interpret local timestamps when they do not contain an explicit offset.
     */
    @JsonAlias({"csvTimezone"})
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
    private Boolean handle;

}
