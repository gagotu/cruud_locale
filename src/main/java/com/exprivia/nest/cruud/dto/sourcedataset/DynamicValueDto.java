package com.exprivia.nest.cruud.dto.sourcedataset;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

/**
 * Modello flessibile per valori dinamici del dataset sorgente: accetta qualsiasi
 * coppia chiave/valore e le espone alla fase di mapping.
 */
public class DynamicValueDto {

    private Map<String, Object> attributes;

    /** Costruttore: inizializza la mappa interna. */
    public DynamicValueDto() {
        this.attributes = new HashMap<>();
    }

    /** Imposta la mappa di attributi completa. */
    public void setAttributes(HashMap<String, Object> attributes) {
        this.attributes = attributes;
    }

    @JsonAnySetter
    public void setAttributes(String key, Object value) {
        this.attributes.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return attributes;
    }

}
