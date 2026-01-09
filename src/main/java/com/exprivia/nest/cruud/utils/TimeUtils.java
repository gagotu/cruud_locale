package com.exprivia.nest.cruud.utils;

import com.exprivia.nest.cruud.dto.urbandataset.values.ResultValueDto;
import lombok.extern.slf4j.Slf4j;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe utility per parsing e normalizzazione dei timestamp usata nella pipeline di conversione.
 * Questa classe gestisce:
 * - composizione di periodi (start_ts/end_ts) a partire da colonne CSV (data + periodo)
 * - parsing di timestamp con o senza offset
 * - normalizzazione verso un offset di destinazione (UD)
 * - gestione di gap/overlap dovuti al cambio DST
 */
@Slf4j
public final class TimeUtils {

    private static final Pattern TIME_COMPONENT_PATTERN = Pattern.compile("\\d{1,2}:\\d{2}");
    private static final Pattern DATE_COMPONENT_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})|(\\d{2}/\\d{2}/\\d{4})|(\\d{8})");

    private TimeUtils() {
    }

    /**
     * Costruisce una mappa periodo con chiavi start_ts/end_ts partendo da:
     * - date: colonna data (puo contenere anche l'orario)
     * - periodHeader: header della colonna periodo (es. "00:00-00:15" o "ea1")
     * - periodValue: valore della colonna periodo (es. "01:15:00")
     * - slice: durata slot in minuti (se >0); se <0 indica un range esplicito
     *
     * Regole principali:
     * - slice < 0: il periodo e nel formato HH:mm-HH:mm nel header.
     * - slice == 0: start=end; se header e un range lo usa direttamente.
     * - slice > 0: il header contiene un indice (es. eaN) da cui derivare i minuti.
     */
    public static HashMap<String, Object> getStartAndEndTimeFromString(String date, String periodHeader, String periodValue, int slice) {
        HashMap<String, Object> period = new HashMap<>();

        if (slice < 0) {
            // Caso "range esplicito": split HH:mm-HH:mm e usa i minuti
            String[] time = periodHeader.split("-");
            String timeEnd = time[1];
            time = time[0].split(":");

            int minutesStart = Integer.parseInt(time[0]) * 60 + Integer.parseInt(time[1]);

            time = timeEnd.split(":");
            int minutesEnd = Integer.parseInt(time[0]) * 60 + Integer.parseInt(time[1]);

            period.put("start_ts", convertMinutesInHoursString(date, minutesStart));
            period.put("end_ts", convertMinutesInHoursString(date, minutesEnd));
        } else if (slice == 0) {
            // Caso "instantaneo": se header contiene un range lo usa, altrimenti start=end
            if (periodHeader != null && periodHeader.contains("-") && periodHeader.contains(":")) {
                String[] time = periodHeader.split("-");
                if (time.length == 2) {
                    String[] startParts = time[0].split(":");
                    String[] endParts = time[1].split(":");
                    int minutesStart = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
                    int minutesEnd = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);
                    period.put("start_ts", convertMinutesInHoursString(date, minutesStart));
                    period.put("end_ts", convertMinutesInHoursString(date, minutesEnd));
                    return period;
                }
            }
            // Se date o periodValue contiene gia timestamp completo lo usa
            String candidate = mergeDateAndTimeColumns(date, periodValue);
            if (candidate != null && !candidate.isBlank()) {
                period.put("start_ts", candidate);
                period.put("end_ts", candidate);
            }
        } else {
            // Caso "slot numerato": cerca il numero nell'header (es. ea1, ea2, ...)
            String regex = "\\d+";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(periodHeader);
            boolean slotResolved = false;

            while (matcher.find()) {
                int number = Integer.parseInt(matcher.group());
                int minutesEnd = number * slice;
                int minutesStart = minutesEnd - slice;

                period.put("start_ts", convertMinutesInHoursString(date, minutesStart));
                period.put("end_ts", convertMinutesInHoursString(date, minutesEnd));
                slotResolved = true;
            }

            if (!slotResolved) {
                // Fallback: prova a comporre date + periodValue
                String candidate = mergeDateAndTimeColumns(date, periodValue);
                if (candidate != null && !candidate.isBlank()) {
                    period.put("start_ts", candidate);
                    period.put("end_ts", candidate);
                }
            }
        }

        enforceSliceDuration(period, slice);
        return period;
    }

    /**
     * Unisce data e periodo considerando i casi in cui uno dei due
     * contenga gia un timestamp completo (data+ora).
     */
    private static String mergeDateAndTimeColumns(String date, String periodValue) {
        String datePart = trimToNull(date);
        String periodPart = trimToNull(periodValue);

        // Se uno dei due e gia completo, lo restituiamo come risultato
        if (isFullTimestamp(datePart)) {
            return datePart;
        }
        if (isFullTimestamp(periodPart)) {
            return periodPart;
        }
        if (datePart == null && periodPart == null) {
            return null;
        }
        if (datePart == null) {
            return periodPart;
        }
        if (periodPart == null) {
            return datePart;
        }

        // Gestione di "T" appesa o prefissa
        String sanitizedDate = trimToNull(removeTrailingT(datePart));
        String sanitizedPeriod = trimToNull(removeLeadingT(periodPart));
        if (sanitizedDate == null && sanitizedPeriod == null) {
            return null;
        }
        if (sanitizedDate == null) {
            return sanitizedPeriod;
        }
        if (sanitizedPeriod == null) {
            return sanitizedDate;
        }

        // Sceglie il separatore coerente con la data originale
        String delimiter;
        if (sanitizedDate.contains("T")) {
            delimiter = "";
        } else if (sanitizedDate.contains(" ")) {
            delimiter = " ";
        } else {
            delimiter = "T";
        }
        return sanitizedDate + delimiter + sanitizedPeriod;
    }

    private static boolean isFullTimestamp(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        // Valuta la presenza di una data e di un componente orario
        return DATE_COMPONENT_PATTERN.matcher(value).find()
                && TIME_COMPONENT_PATTERN.matcher(value).find();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String removeTrailingT(String value) {
        if (value == null) {
            return null;
        }
        if (value.endsWith("T") || value.endsWith("t")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static void enforceSliceDuration(HashMap<String, Object> period, int slice) {
        if (slice <= 0 || period == null) {
            return;
        }
        // Se abbiamo uno start valido, forziamo l'end = start + slice minuti
        Object startObj = period.get("start_ts");
        String start = startObj != null ? startObj.toString() : null;
        if (start == null || start.isBlank()) {
            return;
        }
        String adjusted = addMinutesToTimestamp(start, slice);
        if (adjusted != null) {
            period.put("end_ts", adjusted);
        }
    }

    public static String addMinutesToTimestamp(String timestamp, int minutes) {
        String trimmed = timestamp != null ? timestamp.trim() : null;
        if (trimmed == null || trimmed.isBlank()) {
            return null;
        }
        // Conserva il numero di cifre di frazione (millis/nanos) se presenti
        int fractionDigits = detectFractionDigits(trimmed);
        try {
            // Timestamp con offset esplicito (es. 2023-10-29T01:00:00+02:00)
            OffsetDateTime offset = OffsetDateTime.parse(trimmed);
            DateTimeFormatter formatter = buildFormatter(true, fractionDigits);
            return offset.plusMinutes(minutes).format(formatter);
        } catch (DateTimeParseException ignored) { }
        try {
            // Timestamp locale senza offset
            LocalDateTime local = LocalDateTime.parse(trimmed);
            DateTimeFormatter formatter = buildFormatter(false, fractionDigits);
            return local.plusMinutes(minutes).format(formatter);
        } catch (DateTimeParseException ignored) { }
        return null;
    }

    /**
     * Sottrae 1 centesimo di secondo (10 ms) a un timestamp.
     * Se il timestamp ha una frazione, la mantiene; altrimenti forza almeno 2 cifre.
     */
    public static String subtractCentisecondFromTimestamp(String timestamp) {
        String trimmed = timestamp != null ? timestamp.trim() : null;
        if (trimmed == null || trimmed.isBlank()) {
            return null;
        }
        int fractionDigits = Math.max(2, detectFractionDigits(trimmed));
        try {
            OffsetDateTime offset = OffsetDateTime.parse(trimmed);
            DateTimeFormatter formatter = buildFormatter(true, fractionDigits);
            return offset.minusNanos(10_000_000).format(formatter);
        } catch (DateTimeParseException ignored) { }
        try {
            LocalDateTime local = LocalDateTime.parse(trimmed);
            DateTimeFormatter formatter = buildFormatter(false, fractionDigits);
            return local.minusNanos(10_000_000).format(formatter);
        } catch (DateTimeParseException ignored) { }
        return null;
    }

    /**
     * Rende esclusivo l'end_ts se diverso da start_ts, sottraendo 1 secondo.
     * In ogni caso normalizza end_ts al formato senza millesimi.
     */
    public static void adjustEndExclusive(HashMap<String, Object> period) {
        if (period == null) {
            return;
        }
        Object startObj = period.get("start_ts");
        Object endObj = period.get("end_ts");
        if (startObj == null || endObj == null) {
            return;
        }
        String start = startObj.toString();
        String end = endObj.toString();
        if (start.isBlank() || end.isBlank()) {
            return;
        }

        LocalDateTime startLdt = parseToLocalDateTime(start);
        LocalDateTime endLdt = parseToLocalDateTime(end);
        if (endLdt == null) {
            return;
        }

        if (startLdt != null) {
            if (endLdt.isAfter(startLdt)) {
                endLdt = endLdt.minusSeconds(1);
            } else if (endLdt.isEqual(startLdt)) {
                startLdt = startLdt.withNano(0);
                period.put("start_ts", startLdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                endLdt = startLdt;
            }
        }
        endLdt = endLdt.withNano(0);
        period.put("end_ts", endLdt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    private static LocalDateTime parseToLocalDateTime(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(timestamp).toLocalDateTime();
        } catch (DateTimeParseException ignored) { }
        try {
            return LocalDateTime.parse(timestamp);
        } catch (DateTimeParseException ignored) { }
        return null;
    }

    private static DateTimeFormatter buildFormatter(boolean withOffset, int fractionDigits) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss");
        if (fractionDigits > 0) {
            // Mantiene esattamente il numero di cifre frazionarie rilevate
            builder.appendFraction(ChronoField.NANO_OF_SECOND, fractionDigits, fractionDigits, true);
        }
        if (withOffset) {
            builder.appendOffsetId();
        }
        return builder.toFormatter();
    }

    private static int detectFractionDigits(String value) {
        int dot = value.indexOf('.');
        if (dot < 0) {
            return 0;
        }
        int idx = dot + 1;
        int digits = 0;
        while (idx < value.length()) {
            char c = value.charAt(idx);
            if (!Character.isDigit(c)) {
                break;
            }
            digits++;
            idx++;
        }
        if (digits > 9) {
            digits = 9;
        }
        return digits;
    }

    private static String removeLeadingT(String value) {
        if (value == null) {
            return null;
        }
        if (value.startsWith("T") || value.startsWith("t")) {
            return value.substring(1);
        }
        return value;
    }

    private static String convertMinutesInHoursString(String date, int minutes) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Objects.requireNonNull(getDateTimeFormatter(date)));

        // Se la data include gia un componente orario, la restituiamo invariata
        if (formatter.toString().contains("H")) {
            return LocalDateTime.parse(date, formatter).toString();
        } else {
            // Altrimenti costruisce l'orario a partire dai minuti
            LocalDate localDate = LocalDate.parse(date, formatter);
            LocalDateTime dateTime = LocalDateTime.of(localDate, LocalTime.of(minutes / 60, minutes % 60));
            return dateTime.toString().concat(":00");
        }
    }

    private static String getDateTimeFormatter(String datetimeString) {
        if (datetimeString == null) {
            log.debug("pattern non riconosciuto per valore nullo");
            return "dd/MM/yyyy"; // default pattern
        }

        String sanitized = datetimeString.trim();

        // Prova prima con pattern data+ora
        List<String> dateTimePatterns = List.of(
                "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy HH:mm:ss",
                "yyyyMMddHHmmss",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        );

        for (String pattern : dateTimePatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDateTime.parse(sanitized, formatter);
                return pattern;
            } catch (DateTimeParseException ignored) { }
        }

        // Poi con pattern solo data
        List<String> dateOnlyPatterns = List.of(
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "yyyyMMdd"
        );
        for (String pattern : dateOnlyPatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDate.parse(sanitized, formatter);
                return pattern;
            } catch (DateTimeParseException ignored) { }
        }

        log.debug("pattern non riconosciuto per valore '{}'", sanitized);
        return "dd/MM/yyyy"; // default pattern
    }

    /**
     * Converte una stringa di offset UTC in ZoneOffset.
     * Esempi accettati: "UTC+1", "+01:30", "-2", "Z".
     */
    public static ZoneOffset parseUtcOffset(String utcString) {
        if (utcString == null || utcString.isBlank()) {
            return ZoneOffset.UTC;
        }
        String cleaned = utcString.trim().toUpperCase();
        if (cleaned.startsWith("UTC")) {
            cleaned = cleaned.substring(3).trim();
        }
        if (cleaned.isBlank()) {
            return ZoneOffset.UTC;
        }
        if (cleaned.equals("Z")) {
            return ZoneOffset.UTC;
        }
        if (!cleaned.startsWith("+") && !cleaned.startsWith("-") && !cleaned.isBlank()) {
            cleaned = "+" + cleaned;
        }
        try {
            return ZoneOffset.of(cleaned);
        } catch (DateTimeException ignored) {
            // Fallback al parsing manuale
        }
        try {
            boolean negative = cleaned.startsWith("-");
            String digits = (cleaned.startsWith("+") || cleaned.startsWith("-")) ? cleaned.substring(1) : cleaned;
            if (digits.contains(":")) {
                String[] parts = digits.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                return ZoneOffset.ofHoursMinutes(negative ? -hours : hours, negative ? -minutes : minutes);
            }
            if (digits.length() > 2) {
                int hours = Integer.parseInt(digits.substring(0, digits.length() - 2));
                int minutes = Integer.parseInt(digits.substring(digits.length() - 2));
                return ZoneOffset.ofHoursMinutes(negative ? -hours : hours, negative ? -minutes : minutes);
            }
            int hours = Integer.parseInt(digits);
            return ZoneOffset.ofHours(negative ? -hours : hours);
        } catch (NumberFormatException | DateTimeException ex) {
            log.error("Cannot parse UTC offset string: {}", utcString);
            return ZoneOffset.UTC;
        }
    }

    /**
     * Converte tutti i periodi (start_ts/end_ts) da un offset sorgente a uno target.
     * I timestamp sono interpretati come LocalDateTime nel fuso "utcCsv".
     */
    public static void adjustPeriods(List<ResultValueDto> values, String utcCsv, String utcUd) {
        if (values == null || values.isEmpty()) {
            return;
        }
        ZoneOffset srcOffset = parseUtcOffset(utcCsv);
        ZoneOffset tgtOffset = parseUtcOffset(utcUd);
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        for (ResultValueDto rv : values) {
            if (rv == null) {
                continue;
            }
            HashMap<String, Object> period = rv.getPeriod();
            if (period == null) {
                continue;
            }
            // start_ts
            Object startObj = period.get("start_ts");
            if (startObj instanceof String startStr && !startStr.isBlank()) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(startStr, formatter);
                    Instant instant = ldt.atOffset(srcOffset).toInstant();
                    LocalDateTime converted = LocalDateTime.ofInstant(instant, tgtOffset);
                    period.put("start_ts", converted.format(formatter));
                } catch (Exception e) {
                    log.error("Error adjusting start_ts {}: {}", startObj, e.getMessage());
                }
            }
            // end_ts
            Object endObj = period.get("end_ts");
            if (endObj instanceof String endStr && !endStr.isBlank()) {
                try {
                    LocalDateTime ldt = LocalDateTime.parse(endStr, formatter);
                    Instant instant = ldt.atOffset(srcOffset).toInstant();
                    LocalDateTime converted = LocalDateTime.ofInstant(instant, tgtOffset);
                    period.put("end_ts", converted.format(formatter));
                } catch (Exception e) {
                    log.error("Error adjusting end_ts {}: {}", endObj, e.getMessage());
                }
            }
        }
    }

    /**
     * Converte una stringa IANA (es. "Europe/Rome") in ZoneId.
     */
    public static ZoneId parseZoneId(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        String trimmed = timezone.trim();
        try {
            return ZoneId.of(trimmed);
        } catch (DateTimeException ex) {
            log.error("Cannot parse timezone '{}': {}", timezone, ex.getMessage());
            return ZoneOffset.UTC;
        }
    }

    /**
     * Formatta un Instant nel formato yyyyMMddHHmmss nel fuso target.
     */
    public static String formatAaaaMmGgHhMmSs(Instant instant, ZoneOffset target) {
        if (instant == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return OffsetDateTime.ofInstant(instant, target).format(formatter);
    }

    /**
     * Formatta un Instant come LocalDateTime ISO (senza offset) nel fuso target.
     */
    public static String formatIsoLocalDateTime(Instant instant, ZoneOffset target) {
        if (instant == null) {
            return null;
        }
        return LocalDateTime.ofInstant(instant, target).withNano(0)
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    /**
     * Normalizza un offset UTC in formato "UTC+/-n".
     */
    public static String formatUtcOffsetLabel(String utcString) {
        if (utcString == null || utcString.isBlank()) {
            return null;
        }
        String trimmed = utcString.trim();
        if (trimmed.equalsIgnoreCase("Z")) {
            return "UTC+0";
        }
        if (trimmed.toUpperCase().startsWith("UTC")) {
            String rest = trimmed.substring(3).trim();
            if (rest.isEmpty()) {
                return "UTC+0";
            }
            return "UTC" + rest;
        }
        String sign = (trimmed.startsWith("+") || trimmed.startsWith("-")) ? "" : "+";
        return "UTC" + sign + trimmed;
    }

    /**
     * Risolve un LocalDateTime in ZonedDateTime applicando le regole del fuso.
     * Se handle=false lascia la risoluzione standard (gap/overlap non gestiti).
     * Se handle=true applica scelte deterministiche per overlap e gap.
     */
    public static java.time.ZonedDateTime resolveWithZoneRules(LocalDateTime ldt, ZoneId zone,
                                                               boolean handle, String overlapPolicy, String gapPolicy) {
        ZoneRules rules = zone.getRules();
        List<ZoneOffset> validOffsets = rules.getValidOffsets(ldt);
        if (!handle) {
            if (validOffsets.isEmpty()) {
                ZoneOffsetTransition trans = rules.nextTransition(ldt.minusHours(2).atZone(zone).toInstant());
                if (trans != null) {
                    return trans.getDateTimeAfter().atZone(zone);
                }
                return ldt.atZone(zone);
            } else if (validOffsets.size() > 1) {
                // Overlap: sceglie l'offset "piu tardo" per default
                ZoneOffset preferred = validOffsets.get(validOffsets.size() - 1);
                return java.time.ZonedDateTime.ofLocal(ldt, zone, preferred);
            } else {
                return ldt.atZone(zone);
            }
        }
        if (validOffsets.isEmpty()) {
            // Gap: sposta in avanti al primo istante valido
            ZoneOffsetTransition trans = rules.nextTransition(ldt.minusHours(2).atZone(zone).toInstant());
            if (trans != null) {
                return trans.getDateTimeAfter().atZone(zone);
            }
            return ldt.atZone(zone);
        } else if (validOffsets.size() > 1) {
            // Overlap: seleziona offset in base alla policy richiesta
            ZoneOffset preferred;
            if (overlapPolicy != null && overlapPolicy.equalsIgnoreCase("KEEP_EARLIER")) {
                preferred = validOffsets.get(0).getTotalSeconds() <= validOffsets.get(1).getTotalSeconds()
                        ? validOffsets.get(0) : validOffsets.get(1);
            } else {
                preferred = validOffsets.get(0).getTotalSeconds() >= validOffsets.get(1).getTotalSeconds()
                        ? validOffsets.get(0) : validOffsets.get(1);
            }
            return java.time.ZonedDateTime.ofLocal(ldt, zone, preferred);
        } else {
            return ldt.atZone(zone);
        }
    }

    /**
     * Normalizza tutti i periodi verso un offset UD fisso.
     * Strategia:
     * 1) parse start/end con pattern noti
     * 2) calcola transizioni DST per anno
     * 3) applica correzioni "compress/expand" a partire dai confini DST
     * 4) converte all'offset UD e riscrive le stringhe
     */
    public static void normalizePeriodsForUd(List<ResultValueDto> values, String startKey, String endKey,
                                             ZoneId csvZone, ZoneOffset udOffset, boolean handle,
                                             String overlapPolicy, String gapPolicy) {
        if (values == null || values.isEmpty()) {
            return;
        }
        log.debug("Utils: normalizePeriodsForUd start (values={}, csvZone={}, udOffset={}, handle={})",
                values.size(), csvZone, udOffset, handle);
        List<Integer> years = new ArrayList<>();
        java.util.Set<Integer> yearSet = new java.util.HashSet<>();
        DateTimeFormatter[] patterns = {
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyyMMddHHmmss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        };
        List<LocalDateTime> starts = new ArrayList<>(values.size());
        List<LocalDateTime> ends = new ArrayList<>(values.size());
        for (ResultValueDto rv : values) {
            HashMap<String, Object> period = rv.getPeriod();
            if (period == null) {
                period = new HashMap<>();
                rv.setPeriod(period);
            }
            LocalDateTime startLdt = null;
            LocalDateTime endLdt = null;
            Object s = period.get(startKey);
            if (s instanceof String sStr && !sStr.isBlank()) {
                startLdt = parseLocalDateTime(sStr, patterns);
                if (startLdt != null) {
                    yearSet.add(startLdt.getYear());
                }
            }
            Object e = period.get(endKey);
            if (e instanceof String eStr && !eStr.isBlank()) {
                endLdt = parseLocalDateTime(eStr, patterns);
                if (endLdt != null) {
                    yearSet.add(endLdt.getYear());
                }
            }
            starts.add(startLdt);
            ends.add(endLdt);
        }
        years.addAll(yearSet);
        java.util.Map<Integer, DstTransitions> transitionsMap = new java.util.HashMap<>();
        for (int yr : years) {
            log.debug("Utils: computing DST transitions for {} in {}", yr, csvZone);
            DstTransitions tr = computeDstTransitions(csvZone, yr);
            transitionsMap.put(yr, tr);
        }
        java.util.Map<Integer, YearCorrectionState> stateMap = new java.util.HashMap<>();
        // Seconda passata: applica correzioni e scrive i nuovi valori
        for (int idx = 0; idx < values.size(); idx++) {
            LocalDateTime startLdt = starts.get(idx);
            LocalDateTime endLdt = ends.get(idx);
            ResultValueDto rv = values.get(idx);
            HashMap<String, Object> period = rv.getPeriod();
            if (startLdt != null) {
                int year = startLdt.getYear();
                YearCorrectionState ys = stateMap.computeIfAbsent(year, y -> new YearCorrectionState());
                DstTransitions tr = transitionsMap.get(year);
                if (handle && tr != null) {
                    // Dopo il salto in avanti: comprimi la linea temporale
                    if (!ys.springFixed && tr.springForwardLocal != null
                            && !startLdt.isBefore(tr.springForwardLocal)) {
                        ys.correctionMinutes -= 60;
                        ys.springFixed = true;
                    }
                    // Durante l'overlap: applica l'espansione alla seconda occorrenza
                    if (!ys.autumnFixed && tr.autumnSecondPassLocal != null) {
                        if (ys.autumnState == 0 && !startLdt.isBefore(tr.autumnSecondPassLocal)) {
                            ys.autumnState = 1;
                        } else if (ys.autumnState == 1 && !startLdt.isBefore(tr.autumnSecondPassLocal)) {
                            ys.correctionMinutes += 60;
                            ys.autumnFixed = true;
                            ys.autumnState = 2;
                        }
                    }
                }
                LocalDateTime correctedStart = startLdt.plusMinutes(ys.correctionMinutes);
                java.time.ZonedDateTime startZdt = resolveWithZoneRules(correctedStart, csvZone, handle, overlapPolicy, gapPolicy);
                Instant startInstant = startZdt.toInstant();
                LocalDateTime targetStart = LocalDateTime.ofEpochSecond(startInstant.getEpochSecond(), startInstant.getNano(), udOffset);
                period.put(startKey, targetStart.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            if (endLdt != null) {
                int yearEnd = endLdt.getYear();
                YearCorrectionState ysEnd = stateMap.computeIfAbsent(yearEnd, y -> new YearCorrectionState());
                DstTransitions trEnd = transitionsMap.get(yearEnd);
                if (handle && trEnd != null) {
                    if (!ysEnd.springFixed && trEnd.springForwardLocal != null
                            && !endLdt.isBefore(trEnd.springForwardLocal)) {
                        ysEnd.correctionMinutes -= 60;
                        ysEnd.springFixed = true;
                    }
                    if (!ysEnd.autumnFixed && trEnd.autumnSecondPassLocal != null) {
                        if (ysEnd.autumnState == 0 && !endLdt.isBefore(trEnd.autumnSecondPassLocal)) {
                            ysEnd.autumnState = 1;
                        } else if (ysEnd.autumnState == 1 && !endLdt.isBefore(trEnd.autumnSecondPassLocal)) {
                            ysEnd.correctionMinutes += 60;
                            ysEnd.autumnFixed = true;
                            ysEnd.autumnState = 2;
                        }
                    }
                }
                LocalDateTime correctedEnd = endLdt.plusMinutes(ysEnd.correctionMinutes);
                java.time.ZonedDateTime endZdt = resolveWithZoneRules(correctedEnd, csvZone, handle, overlapPolicy, gapPolicy);
                Instant endInstant = endZdt.toInstant();
                LocalDateTime targetEnd = LocalDateTime.ofEpochSecond(endInstant.getEpochSecond(), endInstant.getNano(), udOffset);
                period.put(endKey, targetEnd.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        }
        log.debug("Utils: normalizePeriodsForUd finish");
    }

    /**
     * Contiene i due punti di transizione rilevanti per un anno:
     * - springForwardLocal: primo orario valido dopo il gap (salto avanti)
     * - autumnSecondPassLocal: inizio della seconda occorrenza (overlap)
     */
    private static final class DstTransitions {
        final LocalDateTime springForwardLocal;
        final LocalDateTime autumnSecondPassLocal;
        DstTransitions(LocalDateTime springForwardLocal, LocalDateTime autumnSecondPassLocal) {
            this.springForwardLocal = springForwardLocal;
            this.autumnSecondPassLocal = autumnSecondPassLocal;
        }
    }

    private static final class YearCorrectionState {
        boolean springFixed = false;
        boolean autumnFixed = false;
        int autumnState = 0; // 0: not seen, 1: first pass, 2: correction applied
        int correctionMinutes = 0;
    }

    /**
     * Calcola le transizioni DST per un anno specifico.
     * Scorre le transizioni della zona finche trova il primo gap e il primo overlap.
     */
    private static DstTransitions computeDstTransitions(ZoneId zone, int year) {
        ZoneRules rules = zone.getRules();
        java.time.ZonedDateTime zStart = LocalDateTime.of(year, 1, 1, 0, 0).atZone(zone);
        int foundSpring = 0;
        LocalDateTime springAfter = null;
        LocalDateTime autumnSecond = null;
        java.time.ZonedDateTime search = zStart;
        while (true) {
            ZoneOffsetTransition trans = rules.nextTransition(search.toInstant());
            if (trans == null) {
                log.debug("Utils: no further transitions after {}", search);
                break;
            }
            LocalDateTime dtBefore = trans.getDateTimeBefore();
            LocalDateTime dtAfter = trans.getDateTimeAfter();
            int transYearBefore = dtBefore.getYear();
            int transYearAfter = dtAfter.getYear();
            if (transYearBefore > year) {
                log.debug("Utils: transition {} beyond year {}, stopping", trans, year);
                break;
            }
            if (trans.isGap()) {
                // Gap (primavera): memorizza il primo orario valido dopo il salto
                if (springAfter == null && (transYearAfter == year || transYearBefore == year)) {
                    springAfter = dtAfter;
                    log.debug("Utils: spring transition for {} -> {}", year, springAfter);
                }
            } else if (trans.isOverlap()) {
                // Overlap (autunno): dtAfter e l'inizio della seconda occorrenza
                if (autumnSecond == null && (transYearAfter == year || transYearBefore == year)) {
                    autumnSecond = dtAfter;
                    log.debug("Utils: autumn transition for {} -> {}", year, autumnSecond);
                }
            }
            if (springAfter != null && autumnSecond != null) {
                log.debug("Utils: collected both transitions for {}, breaking", year);
                break;
            }
            // Avanza oltre la transizione corrente per evitare loop
            search = trans.getDateTimeAfter().plusSeconds(1).atZone(zone);
        }
        return new DstTransitions(springAfter, autumnSecond);
    }

    /**
     * Prova a fare parse con una lista di pattern; torna null se nessuno combacia.
     */
    private static LocalDateTime parseLocalDateTime(String value, DateTimeFormatter[] patterns) {
        for (DateTimeFormatter formatter : patterns) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }
}
