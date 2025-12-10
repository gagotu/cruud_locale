package com.exprivia.nest.cruud.utils;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.dto.ResultUrbanDataset;
import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.context.ContextDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.IdDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.SpecificationDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.PropertyValueDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.exprivia.nest.cruud.dto.urbandataset.values.ResultValueDto;
import java.time.ZoneOffset;
import java.time.OffsetDateTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.zone.ZoneRules;
import java.time.zone.ZoneOffsetTransition;
import java.time.DateTimeException;

/**
 * Utils class for cruud
 */
@Slf4j
public class Utils {

    /**
     * Retrieve start and end time from string and slice
     *
     * @param string from retrieve time
     * @param slice to calculate time
     * @return array times
     */
    public static HashMap<String, Object> getStartAndEndTimeFromString(String date, String string, int slice) {
        HashMap<String, Object> period = new HashMap<>();

        if (slice < 0) {
            String[] time = string.split("-");
            String timeEnd = time[1];
            time = time[0].split(":");

            int minutesStart = Integer.parseInt(time[0]) * 60 + Integer.parseInt(time[1]);

            time = timeEnd.split(":");
            int minutesEnd = Integer.parseInt(time[0]) * 60 + Integer.parseInt(time[1]);

            period.put("start_ts", convertMinutesInHoursString(date, minutesStart));
            period.put("end_ts", convertMinutesInHoursString(date, minutesEnd));
        } else if(slice == 0) {
            period.put("start_ts", convertMinutesInHoursString(date, 0));
            period.put("end_ts", convertMinutesInHoursString(date, 0));
        } else {
            String regex = "\\d+";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(string);

            while (matcher.find()) {
                int number = Integer.parseInt(matcher.group());
                int minutesEnd = number*slice;
                int minutesStart = minutesEnd-slice;

                period.put("start_ts", convertMinutesInHoursString(date, minutesStart));
                period.put("end_ts", convertMinutesInHoursString(date, minutesEnd));
            }
        }

        return period;
    }

    /**
     * Method to get names of files from folder path
     *
     * @param folderPath that include files
     * @return files name
     */
    public static List<String> getFilesNameFromPath(String folderPath) {
        try (Stream<Path> files = Files.list(Paths.get(folderPath))) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();
        } catch (IOException e) {
            log.error("Error: {} -> path doesn't exist or not contains csv files!", e.getMessage());
        }

        return List.of(); //returned void list if files doesn't exist
    }

    /**
     * Method to clean files completed from folder path
     *
     * @param folderPath where clean files
     */
    public static void cleanFilesCompleted(String folderPath) {
        File folder = new File(folderPath);

        // Verifica se il percorso è una cartella esistente
        if (!folder.exists() || !folder.isDirectory()) {
            log.error("The folder '{}' doesn't exists", folderPath);
            return;
        }

        File[] files = folder.listFiles();

        if (files != null) {
            for (File file : files) {
                // Verifica se l'elemento è un file e lo elimina
                if (file.isFile() && !file.delete()) {
                    log.error("Error during delete: {}", file.getName());
                }
            }
        }
    }

    /**
     * Method to create a file into a specific path
     *
     * @param createFilePath file path output
     * @param urbanDataset urban data set that
     * @param objectMapper object to write a json
     *
     * @throws IOException error
     */
    public static void createFile(String createFilePath, ResultUrbanDataset urbanDataset, ObjectMapper objectMapper) throws IOException {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        Path path = Paths.get(createFilePath);

        // Crea le eventuali sottocartelle se non esistono
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }

        objectMapper.writeValue(new File(createFilePath), urbanDataset); //create json file from object
    }

    /**
     * Method to move a file from a path to another
     *
     * @param inputFilePath input file path
     * @param outputFilePath output file path
     *
     * @throws IOException error
     */
    public static void moveFile(String inputFilePath, String outputFilePath) throws IOException {
        Path sourceFile = Paths.get(inputFilePath);
        Path outputFolder = Paths.get(outputFilePath);

        // Crea la sottocartella di destinazione se non esiste
        if (outputFolder.getParent() != null) {
            Files.createDirectories(outputFolder.getParent());
        }

        Files.move(sourceFile, outputFolder, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Update specification and context for UD based on extraction object
     *
     * @param specificationDto to update
     * @param contextDto to update
     * @param extractionDto parametrized
     */
    public static void updateSpecificationAndContext(SpecificationDto specificationDto, ContextDto contextDto, ExtractionDto extractionDto) {
        if (extractionDto.getProducerID() != null) {
            specificationDto.setId(
                    IdDto.builder()
                            .value(extractionDto.getProducerID())
                            .schemeID(specificationDto.getId().getSchemeID())
                            .build()
            );
        } else if(specificationDto != null) {
            String[] pathSplitted = extractionDto.getSourceFilesPath().split("\\\\|/");

            specificationDto.setId(
                    IdDto.builder()
                            .value(pathSplitted[pathSplitted.length-1])
                            .schemeID(specificationDto.getId().getSchemeID())
                            .build()
            );
        }


        if (extractionDto.getCoordinates() != null)
            contextDto.setCoordinates(extractionDto.getCoordinates());

        // Set the target time zone on the UD context if provided on the extraction.
        // The extraction can specify a UTC offset (e.g. "UTC+2") representing the
        // configured UrbanDataset timezone. When present, override the existing
        // timeZone field in the context. This allows downstream conversion logic
        // to operate on a consistent target offset.
        if (extractionDto.getUdUtc() != null && !extractionDto.getUdUtc().isBlank()) {
            contextDto.setTimeZone(extractionDto.getUdUtc());
        }
    }

    /**
     * Method that add null fields into properties
     *
     * @param list propertyValueList
     * @param mapNulls map of nulls
     */
    public static void addNullFields(List<PropertyValueDto> list, Map<String, String> mapNulls) {
        if (mapNulls == null || mapNulls.isEmpty()) {
            return;
        }
        mapNulls.forEach((key, value) -> {
            if (list.stream().noneMatch(obj -> obj.getName().equalsIgnoreCase(key))) {
                list.add(PropertyValueDto.builder().name(key).val(value).build());
            }
        });
    }

    /**
     * Safely convert an arbitrary object into a {@code Map<String, String>} representation.
     * Non-map inputs result in an empty map. Keys and values are stringified; a null value
     * is converted to the literal string "null".
     *
     * @param candidate object to convert (expected Map-like)
     * @return mutable map of string keys and values
     */
    public static Map<String, String> toStringMap(Object candidate) {
        Map<String, String> result = new HashMap<>();
        if (candidate instanceof Map<?, ?> source) {
            source.forEach((key, value) -> result.put(
                    key != null ? key.toString() : null,
                    value != null ? value.toString() : "null"
            ));
        }
        return result;
    }

    /**
     * Method to add new value into property list
     *
     * @param property to find
     * @param header of csv
     * @param line value from csv
     * @param propertiesValue list properties value
     * @param dictionary dictionary from getMapping
     */
    public static void addValueToPropertyList(String property, 
                HashMap<String, LinkedList<Integer>> finalHeaderList, int findValue, HashMap<String, String> line,
                List<PropertyValueDto> propertiesValue, List<ValueDto> dictionary) {

        String[] header = finalHeaderList.keySet().toArray(new String[0]);

        for (int k = 0; k < header.length; k++) {
            var columnName = header[k];

            if (columnName.equalsIgnoreCase(property)) {
                String raw = line.get(columnName);
                if (raw == null) {
                    // Colonna non presente in questa riga: la riempiremo eventualmente con nullsField
                    return;
                }
                propertiesValue.add(PropertyValueDto.builder()
                        .name(columnName)
                        .val(getCorrectValue(columnName, raw, dictionary))
                        .build());
            }
        }

    }

    /**
     * Method to get a correct value after elaboration
     *
     * @param header from csv
     * @param value from csv
     * @param dictionary value from mapping
     * @return string value
     */
    public static String getCorrectValue(String header, String value, List<ValueDto> dictionary) {

        if (value.isBlank())
            return "0";

        for (ValueDto valueDto : dictionary) {
            if (valueDto.getName().equalsIgnoreCase(header) ||
                    (valueDto.getNameForNegative() != null && valueDto.getNameForNegative().equalsIgnoreCase(header))) {

                return (valueDto.getFunction() == null || valueDto.getFunction().isBlank())
                        ? value
                        : Maths.execute(value, valueDto.getFunction(), valueDto.getAlternativeValue());
            }
        }

        return value;

    }

    /**
     * Method to get number or null string value
     *
     * @param value param to evaluate
     * @return correct string value
     */
    public static String getNumberOrNullString(String value, String alternativeResponse) {
        try {
            // Tenta di convertire la stringa in un double.
            // Se la conversione ha successo, significa che la stringa rappresenta un numero.
            Double.parseDouble(value);
            // Se non ci sono eccezioni, restituisci la stringa originale.
            return value;
        } catch (NumberFormatException e) {
            // Se si verifica una NumberFormatException, significa che la stringa non è un numero valido.
            // In questo caso, restituisci la stringa "null".
            return alternativeResponse;
        }
    }

    /**
     * Method to get positive number or null string value
     *
     * @param value param to evaluate
     * @param alternativeResponse alternative response if param doesn't respect
     * @return correct string value
     */
    public static String getPositiveOrNullString(String value, String alternativeResponse) {
        try {
            return Double.parseDouble(value) >= 0 ? value : alternativeResponse;
        } catch (NumberFormatException e) {
            return alternativeResponse;
        }
    }

    private static String convertMinutesInHoursString(String date, int minutes) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Objects.requireNonNull(getDateTimeFormatter(date)));

        if (formatter.toString().contains("H")) { //Se il time è specificato, restituire quest'ultimo
            return LocalDateTime.parse(date, formatter).toString();
        } else {
            LocalDate localDate = LocalDate.parse(date, formatter);
            LocalDateTime dateTime = LocalDateTime.of(localDate, LocalTime.of(minutes/60, minutes%60));
            return dateTime.toString().concat(":00");
        }

    }

    private static String getDateTimeFormatter(String datetimeString) {
        if (datetimeString == null) {
            log.debug("pattern non riconosciuto per valore nullo");
            return "dd/MM/yyyy"; // Pattern default
        }

        String sanitized = datetimeString.trim();

        // First try date+time patterns
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
                return pattern; // match with time component
            } catch (DateTimeParseException ignored) { }
        }

        // Then try date-only patterns
        List<String> dateOnlyPatterns = List.of(
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "yyyyMMdd"
        );
        for (String pattern : dateOnlyPatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                LocalDate.parse(sanitized, formatter);
                return pattern; // match with date only
            } catch (DateTimeParseException ignored) { }
        }

        log.debug("pattern non riconosciuto per valore '{}'", sanitized);
        return "dd/MM/yyyy"; // Pattern default
    }

    /**
     * Parse a string representing a UTC offset into a {@link ZoneOffset}. Supported
     * formats include "UTC+1", "UTC-3", "utc +2". If parsing fails, the
     * returned offset will default to {@link ZoneOffset#UTC}.
     *
     * @param utcString the string to parse
     * @return the parsed {@link ZoneOffset}
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
            // fallthrough to manual parsing
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
     * Adjust the period values of each {@link ResultValueDto} by converting the
     * timestamps from a source UTC offset to a target UTC offset. The period map
     * contains the keys "start_ts" and "end_ts" whose values are ISO-like
     * datetime strings (without offset). This method parses those strings as
     * {@link java.time.LocalDateTime} in the {@code utcCsv} offset, converts
     * to an {@link Instant}, then to a {@link java.time.LocalDateTime} in the
     * {@code utcUd} offset. The converted values are written back into the
     * period map preserving the original format.
     *
     * @param values  list of {@link ResultValueDto} containing periods to adjust
     * @param utcCsv  the source UTC offset string (e.g. "UTC+1")
     * @param utcUd   the target UTC offset string (e.g. "UTC+2")
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
            // adjust start_ts
            Object startObj = period.get("start_ts");
            if (startObj instanceof String startStr && !startStr.isBlank()) {
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(startStr, formatter);
                    Instant instant = ldt.atOffset(srcOffset).toInstant();
                    java.time.LocalDateTime converted = java.time.LocalDateTime.ofInstant(instant, tgtOffset);
                    period.put("start_ts", converted.format(formatter));
                } catch (Exception e) {
                    // Ignore parse errors and leave the original string
                    log.error("Error adjusting start_ts {}: {}", startObj, e.getMessage());
                }
            }
            // adjust end_ts
            Object endObj = period.get("end_ts");
            if (endObj instanceof String endStr && !endStr.isBlank()) {
                try {
                    java.time.LocalDateTime ldt = java.time.LocalDateTime.parse(endStr, formatter);
                    Instant instant = ldt.atOffset(srcOffset).toInstant();
                    java.time.LocalDateTime converted = java.time.LocalDateTime.ofInstant(instant, tgtOffset);
                    period.put("end_ts", converted.format(formatter));
                } catch (Exception e) {
                    log.error("Error adjusting end_ts {}: {}", endObj, e.getMessage());
                }
            }
        }
    }

    /**
     * Parse a String representing an IANA time zone (e.g. "Europe/Rome") into a {@link ZoneId}.
     * If the string is null, blank or invalid, returns {@link ZoneOffset#UTC} as a fallback.
     *
     * @param timezone the IANA time zone ID
     * @return a {@link ZoneId} instance
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
     * Format an {@link Instant} into a string using pattern yyyyMMddHHmmss in the given target offset.
     * This is used to populate the UrbanDataset context timestamp and file names.
     *
     * @param instant the instant to format
     * @param target  the target UTC offset
     * @return formatted string in pattern yyyyMMddHHmmss
     */
    public static String formatAaaaMmGgHhMmSs(Instant instant, ZoneOffset target) {
        if (instant == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        return OffsetDateTime.ofInstant(instant, target).format(formatter);
    }

    /**
     * Resolve a {@link ZonedDateTime} from a local date-time within a time zone, applying optional
     * daylight saving time corrections. When handle is true, gaps (spring forward) and overlaps
     * (autumn) are resolved deterministically according to supplied policies. For gaps the next
     * valid date-time after the transition is returned, effectively compressing the timeline. For
     * overlaps the later offset is chosen (KEEP_LATER), effectively dilating the timeline. When
     * handle is false the date-time is resolved using the default zone rules, which may select
     * the earlier offset in overlaps and leave gaps unhandled.
     *
     * @param ldt           the local date-time in the CSV
     * @param zone          the time zone of the CSV (IANA)
     * @param handle        whether to handle gaps/overlaps
     * @param overlapPolicy policy for overlaps (KEEP_LATER or KEEP_EARLIER) – currently only KEEP_LATER is respected
     * @param gapPolicy     policy for gaps (SHIFT_FORWARD_COMPRESS) – currently only compress is respected
     * @return a {@link ZonedDateTime} representing the resolved instant in the CSV zone
     */
    public static java.time.ZonedDateTime resolveWithZoneRules(java.time.LocalDateTime ldt, ZoneId zone,
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
                ZoneOffset preferred = validOffsets.get(validOffsets.size() - 1);
                return java.time.ZonedDateTime.ofLocal(ldt, zone, preferred);
            } else {
                return ldt.atZone(zone);
            }
        }
        // Determine the valid offsets for this local time
        if (validOffsets.isEmpty()) {
            // Gap: the local time does not exist. Compress timeline by moving forward to next valid transition
            // Equivalent to SHIFT_FORWARD_COMPRESS: compress subsequent times by subtracting 60 minutes.
            // We use nextTransition on the instant just before the local time to find the transition.
            ZoneOffsetTransition trans = rules.nextTransition(ldt.minusHours(2).atZone(zone).toInstant());
            if (trans != null) {
                return trans.getDateTimeAfter().atZone(zone);
            }
            // Fallback: use atZone which will throw DateTimeException, catch and return start of day
            return ldt.atZone(zone);
        } else if (validOffsets.size() > 1) {
            // Overlap: ambiguous local time. Choose later offset (standard time) for KEEP_LATER.
            ZoneOffset preferred;
            if (overlapPolicy != null && overlapPolicy.equalsIgnoreCase("KEEP_EARLIER")) {
                // earlier offset has smaller total seconds
                preferred = validOffsets.get(0).getTotalSeconds() <= validOffsets.get(1).getTotalSeconds()
                        ? validOffsets.get(0) : validOffsets.get(1);
            } else {
                // default KEEP_LATER
                preferred = validOffsets.get(0).getTotalSeconds() >= validOffsets.get(1).getTotalSeconds()
                        ? validOffsets.get(0) : validOffsets.get(1);
            }
            return java.time.ZonedDateTime.ofLocal(ldt, zone, preferred);
        } else {
            // Exactly one valid offset; no gap or overlap
            return ldt.atZone(zone);
        }
    }

    /**
     * Normalize all period timestamps in the given list of {@link ResultValueDto} so that they are
     * expressed relative to a fixed UTC offset. This method performs the following steps for each
     * period:
     * <ul>
     *     <li>Parse the start and end timestamps as local date-times using available patterns.</li>
     *     <li>Apply a correction for DST transitions if handle is true. The correction compresses
     *         spring gaps and dilates autumn overlaps using a single one-hour adjustment per year.</li>
     *     <li>Anchor the corrected local date-times to an {@link Instant} using the source time zone.</li>
     *     <li>Convert the instant to the target offset and write back the formatted local date-time.</li>
     * </ul>
     * The list is mutated in-place. Timestamps that fail to parse will be left unchanged.
     *
     * @param values      list of result values to adjust
     * @param startKey    key in the period map representing the start timestamp (e.g. "start_ts")
     * @param endKey      key in the period map representing the end timestamp (e.g. "end_ts")
     * @param csvZone     the source zone ID (IANA)
     * @param udOffset    the target UTC offset for the UD
     * @param handle      whether to apply DST corrections
     * @param overlapPolicy overlap policy (currently only KEEP_LATER is supported)
     * @param gapPolicy   gap policy (currently only SHIFT_FORWARD_COMPRESS is supported)
     */
    public static void normalizePeriodsForUd(List<ResultValueDto> values, String startKey, String endKey,
                                             ZoneId csvZone, ZoneOffset udOffset, boolean handle,
                                             String overlapPolicy, String gapPolicy) {
        if (values == null || values.isEmpty()) {
            return;
        }
        log.debug("Utils: normalizePeriodsForUd start (values={}, csvZone={}, udOffset={}, handle={})",
                values.size(), csvZone, udOffset, handle);
        // Determine the years present in the dataset
        List<Integer> years = new ArrayList<>();
        // Use a set to avoid duplicates
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
        // First pass: gather years and parse all times
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
        // Precompute DST transitions per year for the CSV zone
        java.util.Map<Integer, DstTransitions> transitionsMap = new java.util.HashMap<>();
        for (int yr : years) {
            log.debug("Utils: computing DST transitions for {} in {}", yr, csvZone);
            DstTransitions tr = computeDstTransitions(csvZone, yr);
            transitionsMap.put(yr, tr);
        }
        // Maintain per-year correction state
        java.util.Map<Integer, YearCorrectionState> stateMap = new java.util.HashMap<>();
        // Iterate values in order; we assume input is roughly sorted by time
        for (int idx = 0; idx < values.size(); idx++) {
            LocalDateTime startLdt = starts.get(idx);
            LocalDateTime endLdt = ends.get(idx);
            ResultValueDto rv = values.get(idx);
            HashMap<String, Object> period = rv.getPeriod();
            // Only adjust if both start and end parse succeeded
            if (startLdt != null) {
                int year = startLdt.getYear();
                YearCorrectionState ys = stateMap.computeIfAbsent(year, y -> new YearCorrectionState());
                DstTransitions tr = transitionsMap.get(year);
                if (handle && tr != null) {
                    // Apply spring correction: compress timeline after spring forward boundary
                    if (!ys.springFixed && tr.springForwardLocal != null
                            && !startLdt.isBefore(tr.springForwardLocal)) {
                        ys.correctionMinutes -= 60;
                        ys.springFixed = true;
                    }
                    // Apply autumn correction: dilate timeline after second pass of overlap
                    if (!ys.autumnFixed && tr.autumnSecondPassLocal != null) {
                        if (ys.autumnState == 0 && !startLdt.isBefore(tr.autumnSecondPassLocal)) {
                            // First entry into overlap; no correction yet
                            ys.autumnState = 1;
                        } else if (ys.autumnState == 1 && !startLdt.isBefore(tr.autumnSecondPassLocal)) {
                            // Second entry; apply correction
                            ys.correctionMinutes += 60;
                            ys.autumnFixed = true;
                            ys.autumnState = 2;
                        }
                    }
                }
                // Apply correction
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
     * Helper class to represent DST transitions for a single year.
     */
    private static final class DstTransitions {
        final LocalDateTime springForwardLocal;
        final LocalDateTime autumnSecondPassLocal;
        DstTransitions(LocalDateTime springForwardLocal, LocalDateTime autumnSecondPassLocal) {
            this.springForwardLocal = springForwardLocal;
            this.autumnSecondPassLocal = autumnSecondPassLocal;
        }
    }

    /**
     * Helper class to maintain per-year correction state during normalization.
     */
    private static final class YearCorrectionState {
        boolean springFixed = false;
        boolean autumnFixed = false;
        int autumnState = 0; // 0: not seen, 1: first pass, 2: correction applied
        int correctionMinutes = 0;
    }

    /**
     * Compute the daylight saving transitions for a given zone and year. Returns a {@link DstTransitions}
     * object containing the first valid local date-time after the spring forward transition and the
     * start of the second pass of the autumn overlap.
     *
     * @param zone the IANA time zone
     * @param year the calendar year
     * @return {@link DstTransitions} for the year, or null if no transitions are found
     */
    private static DstTransitions computeDstTransitions(ZoneId zone, int year) {
        ZoneRules rules = zone.getRules();
        // Start search at beginning of year
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
            // Only consider transitions within the requested year
            if (transYearBefore > year) {
                log.debug("Utils: transition {} beyond year {}, stopping", trans, year);
                break;
            }
            if (trans.isGap()) {
                // Spring forward: gap between before and after
                if (springAfter == null && (transYearAfter == year || transYearBefore == year)) {
                    springAfter = dtAfter;
                    log.debug("Utils: spring transition for {} -> {}", year, springAfter);
                }
            } else if (trans.isOverlap()) {
                // Autumn: overlap between after (start of second pass) and before
                if (autumnSecond == null && (transYearAfter == year || transYearBefore == year)) {
                    autumnSecond = dtAfter;
                    log.debug("Utils: autumn transition for {} -> {}", year, autumnSecond);
                }
            }
            if (springAfter != null && autumnSecond != null) {
                log.debug("Utils: collected both transitions for {}, breaking", year);
                break;
            }
            // Advance search to just after this transition to avoid infinite loop
            search = trans.getDateTimeAfter().plusSeconds(1).atZone(zone);
        }
        return new DstTransitions(springAfter, autumnSecond);
    }

    /**
     * Parse a date-time string using a list of known patterns. Returns null if none match.
     *
     * @param value    the string to parse
     * @param patterns array of formatters to try
     * @return a parsed {@link LocalDateTime} or null
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
