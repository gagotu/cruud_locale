package com.exprivia.nest.cruud.dto;

import com.exprivia.nest.cruud.dto.urbandataset.context.CoordinatesDto;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.*;
import java.util.Date;

/**
 * Rappresenta la configurazione di un'estrazione: dove leggere/scrivere i file,
 * quali mapping usare e come trattare il fuso orario dei dati sorgente e di destinazione.
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
     * Fuso della sorgente CSV/JSON in formato IANA (es. "Europe/Rome").
     * Serve a interpretare i timestamp locali quando non riportano un offset esplicito.
     */
    @JsonAlias({"csvTimezone"})
    private String timeZone;

    /**
     * Offset UTC di destinazione per l'UrbanDataset (es. "+1", "-2", "+01:30").
     * Tutti i timestamp convertiti vengono normalizzati a questo offset.
     */
    private String udUtc;

    /**
     * Se true indica che il file sorgente non porta un offset esplicito: si usa timeZone
     * e si attiva lo scrubbing DST per evitare ambiguità; se false ci si affida all'offset
     * già presente nei dati.
     */
    private Boolean handle;

    /**
     * Abilita la logica speciale per file a fasce orarie (24/48/96 slot per giorno)
     * con doppia riga per gestire l'overlap DST. Disabilitata di default per non
     * impattare gli altri formati.
     */
    @Builder.Default
    private Boolean fasce = Boolean.FALSE;

}
