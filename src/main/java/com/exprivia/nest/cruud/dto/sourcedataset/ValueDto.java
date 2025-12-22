package com.exprivia.nest.cruud.dto.sourcedataset;

import lombok.*;

/**
 * Descrive come interpretare un valore del dataset sorgente: nome di destinazione,
 * eventuale funzione di trasformazione, valore alternativo e gestione coordinate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValueDto {

    @NonNull
    private String name;

    private String function;

    private String nameForNegative;

    private String alternativeValue;

    private Boolean coordinates;

}
