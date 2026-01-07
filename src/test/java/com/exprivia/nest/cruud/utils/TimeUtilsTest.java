package com.exprivia.nest.cruud.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * TimeUtils test class.
 */
@ExtendWith(SpringExtension.class)
public class TimeUtilsTest {

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

        assertEquals(result, TimeUtils.getStartAndEndTimeFromString(date, string, string, slice));
    }

    @Test
    void testGetStartAndEndTimeFromStringWithoutNumber() {
        result.put("start_ts", "2022-07-01T00:00:00");
        result.put("end_ts", "2022-07-01T00:15:00");

        string = "00:00-00:15";
        slice = -1;

        assertEquals(result, TimeUtils.getStartAndEndTimeFromString(date, string, string, slice));
    }

    @Test
    void testGetStartAndEndTimeFromStringWithEaNumberAndBigSlice() {
        result.put("start_ts", "2022-07-01T07:30:00");
        result.put("end_ts", "2022-07-01T09:00:00");

        string = "ea6";
        slice = 90;

        assertEquals(result, TimeUtils.getStartAndEndTimeFromString(date, string, string, slice));
    }

    @Test
    void testGetStartAndEndTimeFromStringMergesDateAndTimeColumns() {
        result.put("start_ts", "2023-10-29T01:00:00.000Z");
        result.put("end_ts", "2023-10-29T01:00:00.000Z");

        assertEquals(result, TimeUtils.getStartAndEndTimeFromString("2023-10-29", "ignored", "01:00:00.000Z", 0));
    }

    @Test
    void testGetStartAndEndTimeFromStringHandlesTrailingTDate() {
        result.put("start_ts", "2023-10-29T01:20:00.000Z");
        result.put("end_ts", "2023-10-29T01:20:00.000Z");

        assertEquals(result, TimeUtils.getStartAndEndTimeFromString("2023-10-29T", "ignored", "01:20:00.000Z", 0));
    }

    @Test
    void testGetStartAndEndTimeFromStringFallsBackToPeriodWhenDateMissing() {
        result.put("start_ts", "2023-10-30T05:15:00+02:00");
        result.put("end_ts", "2023-10-30T05:15:00+02:00");

        assertEquals(result, TimeUtils.getStartAndEndTimeFromString(null, "ignored", "2023-10-30T05:15:00+02:00", 0));
    }

    @Test
    void testGetStartAndEndTimeFromStringKeepsFullDateTimestamp() {
        String fullTs = "2023-10-30T08:00:00";
        result.put("start_ts", fullTs);
        result.put("end_ts", fullTs);

        assertEquals(result, TimeUtils.getStartAndEndTimeFromString(fullTs, "ignored", "01:00:00", 0));
    }

    @Test
    void testSliceAddsEndBasedOnStartWhenHeaderHasNoDigits() {
        HashMap<String, Object> expected = new HashMap<>();
        expected.put("start_ts", "2023-10-29T01:00:00");
        expected.put("end_ts", "2023-10-29T01:20:00");

        assertEquals(expected, TimeUtils.getStartAndEndTimeFromString("2023-10-29", "orario", "01:00:00", 20));
    }

    @Test
    void testSliceAddsEndWhenTimestampHasOffset() {
        HashMap<String, Object> expected = new HashMap<>();
        expected.put("start_ts", "2023-10-29T01:00:00.000Z");
        expected.put("end_ts", "2023-10-29T01:20:00.000Z");

        assertEquals(expected, TimeUtils.getStartAndEndTimeFromString("2023-10-29T", "orario", "01:00:00.000Z", 20));
    }

}
