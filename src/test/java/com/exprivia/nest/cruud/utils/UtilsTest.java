package com.exprivia.nest.cruud.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Utils Test class
 */
@ExtendWith(SpringExtension.class)
public class UtilsTest {

    private String date;
    private String string;
    private int slice;
    private HashMap<String, Object> result;

    @BeforeEach
    void SetUp() {
        date = "01/07/2022";
        string = "ea1";
        slice = 15;
        result = new HashMap<>();
    }

    @Test
    void testGetStartAndEndTimeFromStringWithEaNumber() {
        result.put("start_ts", "2022-07-01T00:00:00");
        result.put("end_ts", "2022-07-01T00:15:00");

        assertEquals(result, Utils.getStartAndEndTimeFromString(date, string, slice));
    }

    @Test
    void testGetStartAndEndTimeFromStringWithoutNumber() {
        result.put("start_ts", "2022-07-01T00:00:00");
        result.put("end_ts", "2022-07-01T00:15:00");

        string = "00:00-00:15";
        slice = -1;

        assertEquals(result, Utils.getStartAndEndTimeFromString(date, string, slice));
    }

    @Test
    void testGetStartAndEndTimeFromStringWithEaNumberAndBigSlice() {
        result.put("start_ts", "2022-07-01T07:30:00");
        result.put("end_ts", "2022-07-01T09:00:00");

        string = "ea6";
        slice = 90;

        assertEquals(result, Utils.getStartAndEndTimeFromString(date, string, slice));
    }

}
