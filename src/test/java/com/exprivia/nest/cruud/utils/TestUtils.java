package com.exprivia.nest.cruud.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test utils class
 */
public class TestUtils {

    /**
     * Convert object into json string
     * @param obj to convert
     * @return json string
     */
    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
