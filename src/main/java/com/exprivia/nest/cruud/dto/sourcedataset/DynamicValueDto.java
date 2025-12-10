package com.exprivia.nest.cruud.dto.sourcedataset;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

import java.util.HashMap;
import java.util.Map;

/**
 * Property Value Dto class for UD-values
 */
public class DynamicValueDto {

    private Map<String, Object> attributes;

    /**
     * Constructor method
     */
    public DynamicValueDto() {
        this.attributes = new HashMap<>();
    }

    /**
     * Set attributes
     *
     * @param attributes to saved
     */
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