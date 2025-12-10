package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.dto.urbandataset.values.ResultValueDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class TimeNormalizationServiceTest {

    private TimeNormalizationService service;

    @BeforeEach
    void setUp() {
        service = new TimeNormalizationService();
    }

    @Test
    void scenario1_explicitUtc_isConverted() {
        List<ResultValueDto> rows = new ArrayList<>();
        rows.add(rowWithTimestamp("2024-10-27T00:30:00Z", 1));

        List<ResultValueDto> normalized = service.normalizePeriods(rows, null, "+1", false);

        assertEquals(1, normalized.size());
        assertEquals("2024-10-27T01:30:00", normalized.get(0).getPeriod().get("start_ts"));
    }

    @Test
    void scenario2_singleOccurrenceInOverlap_isDiscarded() {
        List<ResultValueDto> rows = new ArrayList<>();
        rows.add(rowWithTimestamp("2024-10-27T02:15:00", 1));

        List<ResultValueDto> normalized = service.normalizePeriods(rows, "Europe/Rome", "+1", true);

        assertEquals(0, normalized.size());
    }

    @Test
    void scenario2_doubleSameLocalTime_keepsBothOrdered() {
        List<ResultValueDto> rows = new ArrayList<>();
        rows.add(rowWithTimestamp("2024-10-27T02:15:00", 1));
        rows.add(rowWithTimestamp("2024-10-27T02:15:00", 2));

        List<ResultValueDto> normalized = service.normalizePeriods(rows, "Europe/Rome", "+1", true);

        assertEquals(2, normalized.size());
        String first = (String) normalized.get(0).getPeriod().get("start_ts");
        String second = (String) normalized.get(1).getPeriod().get("start_ts");
        assertNotEquals(first, second);
        assertEquals("2024-10-27T01:15:00", first);
        assertEquals("2024-10-27T02:15:00", second);
    }

    @Test
    void scenario2_tripleSameLocalTime_discardsExtras() {
        List<ResultValueDto> rows = new ArrayList<>();
        rows.add(rowWithTimestamp("2024-10-27T02:20:00", 1));
        rows.add(rowWithTimestamp("2024-10-27T02:20:00", 2));
        rows.add(rowWithTimestamp("2024-10-27T02:20:00", 3));

        List<ResultValueDto> normalized = service.normalizePeriods(rows, "Europe/Rome", "+1", true);

        assertEquals(2, normalized.size());
    }

    @Test
    void scenario2_twoDifferentSinglesInOverlap_allDiscarded() {
        List<ResultValueDto> rows = new ArrayList<>();
        rows.add(rowWithTimestamp("2024-10-27T02:10:00", 1));
        rows.add(rowWithTimestamp("2024-10-27T02:30:00", 2));

        List<ResultValueDto> normalized = service.normalizePeriods(rows, "Europe/Rome", "+1", true);

        assertEquals(0, normalized.size());
    }

    private ResultValueDto rowWithTimestamp(String timestamp, int id) {
        HashMap<String, Object> period = new HashMap<>();
        period.put("start_ts", timestamp);
        period.put("end_ts", timestamp);
        return ResultValueDto.builder().id(id).period(period).build();
    }
}
