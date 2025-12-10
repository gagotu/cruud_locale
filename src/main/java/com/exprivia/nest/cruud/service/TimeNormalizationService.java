package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.dto.urbandataset.values.ResultValueDto;
import com.exprivia.nest.cruud.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.zone.ZoneRules;
import java.util.*;

@Slf4j
@Service
public class TimeNormalizationService {

    private static final DateTimeFormatter OUTPUT_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final List<DateTimeFormatter> LOCAL_PATTERNS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
    );

    public List<ResultValueDto> normalizePeriods(List<ResultValueDto> values,
                                                 String timeZone,
                                                 String udUtcOffset,
                                                 boolean handleDst) {
        return normalizePeriods(values, timeZone, udUtcOffset, handleDst, List.of("start_ts", "end_ts"));
    }

    public List<ResultValueDto> normalizePeriods(List<ResultValueDto> values,
                                                 String timeZone,
                                                 String udUtcOffset,
                                                 boolean handleDst,
                                                 List<String> timestampKeys) {
        if (values == null || values.isEmpty()) {
            return values == null ? List.of() : values;
        }
        if (udUtcOffset == null || udUtcOffset.isBlank()) {
            log.warn("Skipping time normalization: udUtc is missing");
            return values;
        }

        ZoneOffset targetOffset = Utils.parseUtcOffset(udUtcOffset);
        if (!handleDst) {
            log.info("Normalizing timestamps with explicit UTC offsets (targetOffset={})", targetOffset);
            List<ResultValueDto> normalized = normalizeWithExplicitUtc(values, targetOffset, timestampKeys);
            log.info("Normalization completed. kept={} dropped={}", normalized.size(), values.size() - normalized.size());
            return normalized;
        }

        if (timeZone == null || timeZone.isBlank()) {
            log.warn("Cannot handle DST overlap without configured timeZone. Dropping all records to avoid ambiguity.");
            return List.of();
        }

        ZoneId sourceZone = Utils.parseZoneId(timeZone);
        log.info("Normalizing timestamps without UTC using timeZone={} (targetOffset={})", sourceZone, targetOffset);
        List<ResultValueDto> normalized = normalizeWithoutUtc(values, sourceZone, targetOffset, timestampKeys);
        log.info("Normalization completed. kept={} dropped={}", normalized.size(), values.size() - normalized.size());
        return normalized;
    }

    private List<ResultValueDto> normalizeWithExplicitUtc(List<ResultValueDto> values,
                                                          ZoneOffset targetOffset,
                                                          List<String> timestampKeys) {
        List<ResultValueDto> normalized = new ArrayList<>();
        for (ResultValueDto rv : values) {
            HashMap<String, Object> period = rv.getPeriod();
            if (period == null) {
                period = new HashMap<>();
                rv.setPeriod(period);
            }
            boolean drop = false;
            for (String key : timestampKeys) {
                String raw = asString(period.get(key));
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                ZonedDateTime zdt = parseWithOffset(raw);
                if (zdt == null) {
                    logDrop(rv, key, raw, "unable to parse timestamp with explicit UTC/offset");
                    drop = true;
                    break;
                }
                LocalDateTime target = LocalDateTime.ofInstant(zdt.toInstant(), targetOffset);
                period.put(key, target.format(OUTPUT_FORMAT));
            }
            if (!drop) {
                normalized.add(rv);
            }
        }
        return normalized;
    }

    private List<ResultValueDto> normalizeWithoutUtc(List<ResultValueDto> values,
                                                     ZoneId sourceZone,
                                                     ZoneOffset targetOffset,
                                                     List<String> timestampKeys) {
        ZoneRules rules = sourceZone.getRules();
        Map<ResultValueDto, List<TimestampRef>> refsByRecord = new LinkedHashMap<>();
        Map<OverlapKey, List<TimestampRef>> overlapGroups = new LinkedHashMap<>();

        for (ResultValueDto rv : values) {
            HashMap<String, Object> period = rv.getPeriod();
            if (period == null) {
                period = new HashMap<>();
                rv.setPeriod(period);
            }
            List<TimestampRef> refs = new ArrayList<>();
            refsByRecord.put(rv, refs);
            for (String key : timestampKeys) {
                String raw = asString(period.get(key));
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                LocalDateTime ldt = parseLocal(raw);
                if (ldt == null) {
                    refs.add(TimestampRef.drop(key, raw, "unparsable local timestamp"));
                    continue;
                }
                List<ZoneOffset> offsets = rules.getValidOffsets(ldt);
                if (offsets.isEmpty()) {
                    refs.add(TimestampRef.drop(key, raw, "gap in timezone rules"));
                    continue;
                }
                TimestampRef ref = TimestampRef.normal(key, raw, ldt);
                if (offsets.size() == 1) {
                    ref.resolved = ldt.atZone(sourceZone);
                } else {
                    overlapGroups.computeIfAbsent(new OverlapKey(key, ldt), k -> new ArrayList<>()).add(ref);
                }
                refs.add(ref);
            }
        }

        for (Map.Entry<OverlapKey, List<TimestampRef>> entry : overlapGroups.entrySet()) {
            LocalDateTime ldt = entry.getKey().local();
            List<ZoneOffset> offsets = rules.getValidOffsets(ldt);
            if (offsets.size() < 2) {
                entry.getValue().forEach(ref -> ref.dropReason = "overlap detection failed");
                continue;
            }
            ZonedDateTime first = ZonedDateTime.ofLocal(ldt, sourceZone, offsets.get(0));
            ZonedDateTime second = ZonedDateTime.ofLocal(ldt, sourceZone, offsets.get(1));
            if (second.toInstant().isBefore(first.toInstant())) {
                ZonedDateTime tmp = first;
                first = second;
                second = tmp;
            }
            List<TimestampRef> occurrences = entry.getValue();
            if (occurrences.size() == 1) {
                occurrences.get(0).dropReason = "single occurrence in DST overlap without UTC";
            } else {
                occurrences.get(0).resolved = first;
                occurrences.get(1).resolved = second;
                if (occurrences.size() > 2) {
                    for (int i = 2; i < occurrences.size(); i++) {
                        occurrences.get(i).dropReason = "extra occurrences for same local time in DST overlap";
                    }
                }
            }
        }

        List<ResultValueDto> normalized = new ArrayList<>();
        for (Map.Entry<ResultValueDto, List<TimestampRef>> entry : refsByRecord.entrySet()) {
            ResultValueDto rv = entry.getKey();
            List<TimestampRef> refs = entry.getValue();
            boolean drop = false;
            for (TimestampRef ref : refs) {
                if (ref.dropReason != null) {
                    drop = true;
                    logDrop(rv, ref);
                } else if (ref.resolved == null) {
                    drop = true;
                    logDrop(rv, ref.key, ref.raw, "timestamp unresolved after overlap handling");
                }
            }
            if (drop) {
                continue;
            }
            for (TimestampRef ref : refs) {
                if (ref.resolved == null) {
                    continue;
                }
                LocalDateTime target = LocalDateTime.ofInstant(ref.resolved.toInstant(), targetOffset);
                rv.getPeriod().put(ref.key, target.format(OUTPUT_FORMAT));
            }
            normalized.add(rv);
        }

        return normalized;
    }

    private ZonedDateTime parseWithOffset(String raw) {
        try {
            return OffsetDateTime.parse(raw).toZonedDateTime();
        } catch (DateTimeParseException ignored) { }
        try {
            return ZonedDateTime.parse(raw);
        } catch (DateTimeParseException ignored) { }
        try {
            return Instant.parse(raw).atZone(ZoneOffset.UTC);
        } catch (DateTimeParseException ignored) { }
        return null;
    }

    private LocalDateTime parseLocal(String raw) {
        for (DateTimeFormatter formatter : LOCAL_PATTERNS) {
            try {
                return LocalDateTime.parse(raw, formatter);
            } catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    private String asString(Object val) {
        if (val == null) {
            return null;
        }
        return val instanceof String s ? s : val.toString();
    }

    private void logDrop(ResultValueDto rv, TimestampRef ref) {
        logDrop(rv, ref.key, ref.raw, ref.dropReason);
    }

    private void logDrop(ResultValueDto rv, String key, String raw, String reason) {
        log.warn("Dropping record for DST scrubbing id={} key={} timestamp={} reason={}",
                rv != null ? rv.getId() : null, key, raw, reason);
    }

    private static final class TimestampRef {
        final String key;
        final String raw;
        final LocalDateTime local;
        ZonedDateTime resolved;
        String dropReason;

        private TimestampRef(String key, String raw, LocalDateTime local) {
            this.key = key;
            this.raw = raw;
            this.local = local;
        }

        static TimestampRef drop(String key, String raw, String reason) {
            TimestampRef ref = new TimestampRef(key, raw, null);
            ref.dropReason = reason;
            return ref;
        }

        static TimestampRef normal(String key, String raw, LocalDateTime local) {
            return new TimestampRef(key, raw, local);
        }
    }

    private record OverlapKey(String key, LocalDateTime local) { }
}
