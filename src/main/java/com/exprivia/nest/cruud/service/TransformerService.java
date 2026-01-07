package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.dto.*;
import com.exprivia.nest.cruud.dto.sourcedataset.ValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.UrbanDatasetDto;
import com.exprivia.nest.cruud.dto.urbandataset.context.ContextDto;
import com.exprivia.nest.cruud.dto.urbandataset.context.CoordinatesDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.PropertiesDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.SpecificationDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.PropertyValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.ResultValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.ValuesDto;
import com.exprivia.nest.cruud.utils.FileUtils;
import com.exprivia.nest.cruud.utils.MappingUtils;
import com.exprivia.nest.cruud.utils.TimeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.opencsv.*;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * Servizio che orchestra la trasformazione da CSV/JSON sorgente verso l'UrbanDataset.
 * Carica le proprietà, costruisce periodi e valori a partire dai mapping di configurazione,
 * e delega la normalizzazione temporale al TimeNormalizationService per garantire offset UD coerenti.
 */
@SuppressWarnings("unused")
@Slf4j
@Service
public class TransformerService {

    private  static final String COMPLETED_PATH = "/completed/";
    public static final String FORMAT = "format";
    public static final String LATITUDE = "latitude";
    public static final String HEIGHT = "height";
    public static final String LONGITUDE = "longitude";

    @Value("${temp-folder}")
    private String tempFolderPath;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private TimeNormalizationService timeNormalizationService;

    // Maximum number of rows allowed in a single UrbanDataset. When greater than zero, the
    // conversion will produce multiple UD files if the values list exceeds this threshold.
    @Value("${ud.maxRowsPerUd:0}")
    private int maxRowsPerUd;

    /**
     * Method to convert a csv into UD OWL json
     *
     * @return data converted
     */
    public RequestResultDto executeConversionFromFolder(ExtractionDto extractionDto) {
        log.debug("Transformer Service: Transformer -> {}", extractionDto);

        List<String> filesRetrieved = FileUtils.getFilesNameFromPath(extractionDto.getSourceFilesPath());
        log.debug("Transformer Service: files retrieved from {} -> {}", extractionDto.getSourceFilesPath(), filesRetrieved);

        RequestResultDto result = RequestResultDto.builder().build();

        if (!filesRetrieved.isEmpty()) {

            result.setFilesCompleted(filesRetrieved);

            for (String fileName : filesRetrieved) {
                String inputFilePath = extractionDto.getSourceFilesPath() + '/' + fileName;

                log.debug("Transformer Service: start conversion for file {}", inputFilePath);
                ResultUrbanDataset resultUrbanDataset = convertCsvToUrbanDataset(extractionDto, inputFilePath);
                try {
                    writeJsonFile(resultUrbanDataset, extractionDto, inputFilePath, fileName); // write json result into file
                    log.debug("Transformer Service: conversion completed for file {}", fileName);
                } catch (IOException e) {
                    log.error("Error during write json results");
                    throw new RuntimeException(e);
                }
            }

        }

        // End values UD
        result.setDescription("Execution completed for " + filesRetrieved.size() + " files");
        return result;
    }

    /**
     * Method to execute csv conversions from extraction data retrieved by extractionName
     *
     * @param extractionName name of extraction
     *
     * @return operation result
     */
    public RequestResultDto executeConversionFromExtraction(String extractionName) {
        log.debug("Transformer Service: TransformerByExtraction -> {}", extractionName);

        Optional<ExtractionDto> extractionDto = extractionService.getByExtractionName(extractionName);

        if (extractionDto.isPresent()) {
            return executeConversionFromFolder(extractionDto.get());
        } else {
            return RequestResultDto.builder()
                    .filesCompleted(List.of()).description("Extraction with name: " + extractionName + " not found!")
                    .build();
        }
    }

    /**
     * Method to execute extraction from file upload
     *
     * @param file to convert
     * @param extractionName extraction name
     * @return urban data set
     */
    public ResultUrbanDataset executeConversionFromUpload(MultipartFile file, String extractionName) {
        if (file.isEmpty()){
            return ResultUrbanDataset.builder().build();
        }

        ResultUrbanDataset resultUrbanDataset;

        // Ottengo il nome del file originale
        String fileName = file.getOriginalFilename();
        // Ottengo i byte del file
        byte[] fileBytes;

        try {
            fileBytes = file.getBytes();

            Path tempFile = Paths.get(tempFolderPath + fileName);

            // Crea la sottocartella di destinazione se non esiste
            if (tempFile.getParent() != null) {
                Files.createDirectories(tempFile.getParent());
            }

            Files.write(tempFile, fileBytes);

            Optional<ExtractionDto> extractionDto = extractionService.getByExtractionName(extractionName);

            resultUrbanDataset = convertCsvToUrbanDataset(extractionDto.get(), tempFile.toString());

        } catch (IOException e) {
            log.error("Error during reading/writing file: {}", e.getMessage());
            throw new RuntimeException(e);
        }

        FileUtils.cleanFilesCompleted(tempFolderPath); //remove temporary file used.

        return resultUrbanDataset;

    }

    //Csv Transform Engine
    private ResultUrbanDataset convertCsvToUrbanDataset(ExtractionDto extractionDto, String inputFilePath) {
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        PropertyDto propertyDto = retrievePropertyFromFilter(extractionDto); //retrieve found property
        if (propertyDto == null) {
            throw new IllegalStateException("Property not found for name " + extractionDto.getPropertyName());
        }
        log.debug("Transformer Service: property '{}' loaded (mappings: {})", propertyDto.getName(), propertyDto.getMappings() != null ? propertyDto.getMappings().keySet() : "none");

        ResultUrbanDataset resultUrbanDataset = null; // define a new Result Urban Dataset
        int sliceMinutes = resolveSliceMinutes(propertyDto);

        // Start prepared UD

        SpecificationDto specificationDto;
        ContextDto contextDto;

        try {
            specificationDto = objectMapper.readValue(
                    objectMapper.writeValueAsString(propertyDto.getSpecification()), SpecificationDto.class
            );

            contextDto = objectMapper.readValue(
                    objectMapper.writeValueAsString(propertyDto.getContext()), ContextDto.class
            );
        } catch (JsonProcessingException e) {
            log.error("Error during retrieve and set specification/context for Urban Dataset");
            throw new RuntimeException(e);
        }

        MappingUtils.updateSpecificationAndContext(specificationDto, contextDto, extractionDto);

        UrbanDatasetDto urbanDataset = UrbanDatasetDto.builder()
                .specification(specificationDto)
                .context(contextDto)
                .build();

        // End prepared UD

        // Start values UD

        try {
            // retrieve single values data of UD
            List<ResultValueDto> values = retrieveValueFromCsv(extractionDto, propertyDto, inputFilePath);
            log.debug("Transformer Service: retrieved {} rows from {}", values != null ? values.size() : 0, inputFilePath);
            // Normalize period timestamps according to the target UD UTC offset and configured time zone handling
            String udUtcStr = extractionDto.getUdUtc();
            boolean handleDst = extractionDto.getHandle() != null && extractionDto.getHandle();
            List<String> timestampKeys = sliceMinutes > 0 ? List.of("start_ts") : List.of("start_ts", "end_ts");
            if (udUtcStr != null && !udUtcStr.isBlank()) {
                int originalSize = values != null ? values.size() : 0;
                boolean allowSingleOverlap = extractionDto.getFasce() != null && extractionDto.getFasce();
                values = timeNormalizationService.normalizePeriods(values, extractionDto.getTimeZone(), udUtcStr, handleDst, timestampKeys, allowSingleOverlap);
                log.debug("Transformer Service: periods normalized (kept {} of {})", values != null ? values.size() : 0, originalSize);
            }
            if (sliceMinutes > 0 && !Boolean.TRUE.equals(extractionDto.getFasce())) {
                enforceSliceDurationAfterNormalization(values, sliceMinutes);
            }
            if (Boolean.TRUE.equals(extractionDto.getFasce())) {
                values = adjustSlotDurationsAndSort(values);
            }
            urbanDataset.setValues(ValuesDto.builder().line(values).build());
            log.debug("Transformer Service: building urban dataset with {} values", values != null ? values.size() : null);
            resultUrbanDataset = ResultUrbanDataset.builder().urbanDataset(urbanDataset).build();
            log.debug("Transformer Service: urban dataset ready");
        } catch (IOException | CsvValidationException e) {
            log.error("Error reading/writing from CSV: {}", e.getMessage());
        }
        // End values UD

        return resultUrbanDataset;
    }

    // From this method, we can retrieve data from CSV file
    @SuppressWarnings("unchecked")
    private List<ResultValueDto> retrieveValueFromCsv(ExtractionDto retrievePropertyFilterDto, PropertyDto propertyDto, String inputFilePath) throws IOException, CsvValidationException {

        List<ResultValueDto> values = new ArrayList<>(); // initialize result values

        FileReader fileReader = new FileReader(inputFilePath);

        CSVReader reader = new CSVReaderBuilder(fileReader)
                .withCSVParser(new CSVParserBuilder().withSeparator(retrievePropertyFilterDto.getSeparator()).build())
                .build();

        String[] line;

        String[] header = reader.readNext();

        String[] originalHeader = Arrays.copyOf(header, header.length);

        HashMap<String, LinkedList<Integer>> finalHeaderToLine = new HashMap<>();

        convertedHeaderFromMappings(originalHeader, propertyDto.getMappings(), finalHeaderToLine); //done converted header

        SpecificationDto specificationDto = objectMapper.readValue(
                objectMapper.writeValueAsString(propertyDto.getSpecification()), SpecificationDto.class
        );

        List<String> propertiesWithoutSubs = new ArrayList<>(specificationDto.getProperties().getPropertyDefinition().stream()
                .filter(prp -> prp.getSubProperties() == null)
                .map(PropertiesDto::getPropertyName)
                .toList());

        List<String> subProperties = specificationDto.getProperties().getPropertyDefinition().stream()
                .filter(prp -> prp.getSubProperties() != null)
                .flatMap(prp -> prp.getSubProperties().getPropertyName().stream())
                .toList();

        propertiesWithoutSubs.removeAll(subProperties);

        Map<String, Object> configurations = propertyDto.getConfigurations() != null
                ? propertyDto.getConfigurations()
                : new HashMap<>();

        List<String> periods = (List<String>) configurations.get("period");
        List<Integer> columnsPeriods = retrieveColumnsNumberForHeader(periods, originalHeader);
        int slice = (int) configurations.getOrDefault("slice", -1);
        boolean slotMode = Boolean.TRUE.equals(retrievePropertyFilterDto.getFasce());

        int dateColumn = -1;
        if (configurations.containsKey("date")) {
            dateColumn = retrieveColumnsNumberForHeader(List.of(configurations.get("date").toString()), originalHeader).getFirst();
        }

        Map<String, String> nullsFieldMap = MappingUtils.toStringMap(configurations.get("nullsField"));

        int id = 1;
        HashMap<String, Object> period;

        if (slotMode && !columnsPeriods.isEmpty() && dateColumn >= 0) {
            List<String[]> allLines = new ArrayList<>();
            while ((line = reader.readNext()) != null) {
                allLines.add(line);
            }
            LinkedHashMap<String, List<String[]>> rowsByDate = new LinkedHashMap<>();
            for (String[] l : allLines) {
                if (l.length <= dateColumn || l[dateColumn] == null || l[dateColumn].isBlank()) {
                    continue;
                }
                rowsByDate.computeIfAbsent(l[dateColumn], k -> new ArrayList<>()).add(l);
            }
            for (Map.Entry<String, List<String[]>> entry : rowsByDate.entrySet()) {
                List<String[]> dayLines = entry.getValue();
                if (dayLines.size() > 2) {
                    log.warn("Found more than two rows for date {} in slot mode: keeping first two, discarding {}", entry.getKey(), dayLines.size() - 2);
                    dayLines = dayLines.subList(0, 2);
                }
                String[] firstLine = dayLines.get(0);
                String[] secondLine = dayLines.size() > 1 ? dayLines.get(1) : null;
                for (int i = 0; i < columnsPeriods.size(); i++) {
                    int colIdx = columnsPeriods.get(i);
                    if (colIdx < 0) {
                        continue;
                    }
                    String periodHeader = originalHeader[colIdx];
                    String v1 = colIdx < firstLine.length ? firstLine[colIdx] : null;
                    String v2 = secondLine != null && colIdx < secondLine.length ? secondLine[colIdx] : null;
                    boolean v1Has = hasNonZeroValue(v1);
                    boolean v2Has = hasNonZeroValue(v2);

                    if (v1Has && v2Has) {
                        id = appendSlotRecord(firstLine, firstLine[dateColumn], periodHeader, colIdx, slice,
                                propertiesWithoutSubs, propertyDto.getMappings().values().stream().toList(), finalHeaderToLine, nullsFieldMap,
                                values, propertyDto, subProperties, id);
                        id = appendSlotRecord(secondLine, firstLine[dateColumn], periodHeader, colIdx, slice,
                                propertiesWithoutSubs, propertyDto.getMappings().values().stream().toList(), finalHeaderToLine, nullsFieldMap,
                                values, propertyDto, subProperties, id);
                    } else if (v1Has || v2Has) {
                        String[] src = v1Has ? firstLine : secondLine;
                        id = appendSlotRecord(src, firstLine[dateColumn], periodHeader, colIdx, slice,
                                propertiesWithoutSubs, propertyDto.getMappings().values().stream().toList(), finalHeaderToLine, nullsFieldMap,
                                values, propertyDto, subProperties, id);
                    } else {
                        // entrambi null/zero: genero un solo record nullo
                        id = appendSlotRecord(firstLine, firstLine[dateColumn], periodHeader, colIdx, slice,
                                propertiesWithoutSubs, propertyDto.getMappings().values().stream().toList(), finalHeaderToLine, nullsFieldMap,
                                values, propertyDto, subProperties, id);
                    }
                }
            }
        } else {
            while ((line = reader.readNext()) != null) {
                if (!columnsPeriods.isEmpty()) {
                    for (int i=0; i<columnsPeriods.size(); i++) {

                        if (line[dateColumn] != null && !line[dateColumn].isBlank()) {
                            //Effettua le operazioni solo se il datetime è valorizzato
                            ResultValueDto resultValueDto = ResultValueDto.builder().id(id).build();

                            String periodHeader = originalHeader[columnsPeriods.get(i)];
                            String periodValue = line[columnsPeriods.get(i)];
                            period = TimeUtils.getStartAndEndTimeFromString(line[dateColumn], periodHeader, periodValue, slice);

                            if (!period.isEmpty())
                                resultValueDto.setPeriod(period);

                            int columnDataPeriod = -1;
                            if (periods.size() > 1) // if there are more of 1 date column
                                columnDataPeriod = retrieveColumnDataFromPeriod(periods.get(i), originalHeader);

                            resultValueDto.setProperty(processPropertiesValue(line, columnDataPeriod, propertiesWithoutSubs, propertyDto.getMappings().values().stream().toList(), finalHeaderToLine, nullsFieldMap));

                            if (elaborateCoordinates(subProperties)) {
                                resultValueDto.setCoordinates(getCoordinatesFromLine(finalHeaderToLine, line));
                            }

                            id = writeResultValueDto(propertyDto, resultValueDto, values, id);
                        }

                    }
                } else {
                    ResultValueDto resultValueDto = ResultValueDto.builder().id(id).build();

                    List<Integer> columnsToRead = retrieveColumnsNumberForHeader(propertiesWithoutSubs, originalHeader);

                    resultValueDto.setProperty(processPropertiesValue(line, columnsToRead.isEmpty() ? -1 : columnsToRead.getFirst(), propertiesWithoutSubs, propertyDto.getMappings().values().stream().toList(), finalHeaderToLine, nullsFieldMap));

                    if (elaborateCoordinates(subProperties)) {
                        resultValueDto.setCoordinates(getCoordinatesFromLine(finalHeaderToLine, line));
                    }

                    id = writeResultValueDto(propertyDto, resultValueDto, values, id);
                }

            }
        }

        reader.close(); //close reader csv
        fileReader.close(); //close file

        return values;
    }

    /**
     * Genera un record a partire da una riga slot e da un indice colonna specifico.
     */
    private int appendSlotRecord(String[] sourceLine,
                                 String dateValue,
                                 String periodHeader,
                                 int columnIndex,
                                 int slice,
                                 List<String> propertiesWithoutSubs,
                                 List<ValueDto> dictionary,
                                 HashMap<String, LinkedList<Integer>> finalHeaderToLine,
                                 Map<String, String> nullsFieldMap,
                                 List<ResultValueDto> values,
                                 PropertyDto propertyDto,
                                 List<String> subProperties,
                                 int id) throws JsonProcessingException {
        if (sourceLine == null) {
            return id;
        }
        if (columnIndex >= sourceLine.length) {
            log.warn("Skipping slot column {} because line has only {} columns", columnIndex, sourceLine.length);
            return id;
        }
        String[] lineCopy = Arrays.copyOf(sourceLine, Math.max(sourceLine.length, columnIndex + 1));
        ResultValueDto resultValueDto = ResultValueDto.builder().id(id).build();

        HashMap<String, Object> period = TimeUtils.getStartAndEndTimeFromString(dateValue, periodHeader, lineCopy[columnIndex], slice);
        if (!period.isEmpty()) {
            resultValueDto.setPeriod(period);
        }

        resultValueDto.setProperty(processPropertiesValue(lineCopy, columnIndex, propertiesWithoutSubs, dictionary, finalHeaderToLine, nullsFieldMap));

        if (elaborateCoordinates(subProperties)) {
            resultValueDto.setCoordinates(getCoordinatesFromLine(finalHeaderToLine, lineCopy));
        }

        return writeResultValueDto(propertyDto, resultValueDto, values, id);
    }

    private boolean hasNonZeroValue(String raw) {
        if (raw == null || raw.isBlank()) {
            return false;
        }
        try {
            double d = Double.parseDouble(MappingUtils.normalizeDecimalSeparator(raw));
            return d != 0d;
        } catch (NumberFormatException ex) {
            // Se non parsabile, consideralo valorizzato per non perdere il dato
            return true;
        }
    }

    /**
     * Rileva se lo slot cade in una finestra di overlap DST per la timeZone indicata.
     */
    private boolean isOverlapSlot(String dateStr, String periodHeader, String timeZone, int slice) {
        if (timeZone == null || timeZone.isBlank() || dateStr == null || dateStr.isBlank() || periodHeader == null) {
            return false;
        }
        try {
            ZoneId zone = TimeUtils.parseZoneId(timeZone);
            HashMap<String, Object> period = TimeUtils.getStartAndEndTimeFromString(dateStr, periodHeader, null, slice);
            Object startObj = period.get("start_ts");
            if (startObj == null) {
                return false;
            }
            LocalDateTime ldt = LocalDateTime.parse(startObj.toString());
            return zone.getRules().getValidOffsets(ldt).size() > 1;
        } catch (Exception e) {
            return false;
        }
    }

    private Boolean elaborateCoordinates(List<String> strings) {
        return strings.stream().anyMatch(string -> string.equalsIgnoreCase(FORMAT)
                || string.equalsIgnoreCase(LATITUDE)
                || string.equalsIgnoreCase(HEIGHT)
                || string.equalsIgnoreCase(LONGITUDE));
    }

    private CoordinatesDto getCoordinatesFromLine(HashMap<String, LinkedList<Integer>> headerToLine, String[] line) {
        CoordinatesDto coordinatesDto = CoordinatesDto.builder()
                .format(headerToLine.containsKey(FORMAT) ? line[headerToLine.get(FORMAT).getFirst()] : "WGS84-DD")
                .height(headerToLine.containsKey(HEIGHT) ? Double.parseDouble(line[headerToLine.get(HEIGHT).getFirst()]) : 0D)
                .longitude(headerToLine.containsKey(LONGITUDE) ? Double.parseDouble(line[headerToLine.get(LONGITUDE).getFirst()]) : 0D)
                .latitude(headerToLine.containsKey(LATITUDE) ? Double.parseDouble(line[headerToLine.get(LATITUDE).getFirst()]) : 0D)
                .build();

        return coordinatesDto;
    }

    private int writeResultValueDto(PropertyDto propertyDto, ResultValueDto resultValueDto, List<ResultValueDto> values, int id) throws JsonProcessingException {
        //TODO: Write coordinates only if there are into mappings
        /*if (propertyDto.getContext().containsKey("coordinates")) {
            CoordinatesDto coordinatesDto = objectMapper.readValue(objectMapper.writeValueAsString(propertyDto.getContext().get("coordinates")), CoordinatesDto.class);
            resultValueDto.setCoordinates(coordinatesDto);
        }*/

        values.add(resultValueDto);

        return ++id;
    }

    /**
     * In modalità fasce, forza la durata della fascia a quella indicata dall'header
     * (in minuti) e riordina i record per start_ts assegnando id sequenziali.
     */
    private List<ResultValueDto> adjustSlotDurationsAndSort(List<ResultValueDto> values) {
        if (values == null || values.isEmpty()) {
            return values;
        }
        long slotMinutes = inferSlotMinutes(values);
        for (ResultValueDto v : values) {
            if (v.getPeriod() == null) {
                continue;
            }
            Object startObj = v.getPeriod().get("start_ts");
            if (startObj == null) {
                continue;
            }
            LocalDateTime start = parseLdt(startObj.toString());
            if (start == null) {
                continue;
            }
            LocalDateTime end = start.plusMinutes(slotMinutes);
            v.getPeriod().put("end_ts", end.toString());
        }
        values.sort(Comparator.comparing(v -> parseLdt(v.getPeriod() != null ? Objects.toString(v.getPeriod().get("start_ts"), null) : null),
                Comparator.nullsLast(Comparator.naturalOrder())));
        int newId = 1;
        for (ResultValueDto v : values) {
            v.setId(newId++);
        }
        return values;
    }

    /**
     * Deduce la durata dello slot cercando la minima differenza positiva tra start ed end.
     * Se non disponibile, default 15 minuti.
     */
    private long inferSlotMinutes(List<ResultValueDto> values) {
        long best = Long.MAX_VALUE;
        for (ResultValueDto v : values) {
            if (v.getPeriod() == null) {
                continue;
            }
            Object startObj = v.getPeriod().get("start_ts");
            Object endObj = v.getPeriod().get("end_ts");
            if (startObj == null || endObj == null) {
                continue;
            }
            LocalDateTime start = parseLdt(startObj.toString());
            LocalDateTime end = parseLdt(endObj.toString());
            if (start == null || end == null) {
                continue;
            }
            long minutes = Duration.between(start, end).toMinutes();
            if (minutes > 0 && minutes < best) {
                best = minutes;
            }
        }
        return best == Long.MAX_VALUE ? 15 : best;
    }

    private LocalDateTime parseLdt(String ts) {
        if (ts == null || ts.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(ts, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            log.warn("Impossible parsing timestamp {} as LocalDateTime", ts);
            return null;
        }
    }

    private List<PropertyValueDto> processPropertiesValue(String[] originalLine, int columnToRead,
                    List<String> propertiesWithoutSubs, List<ValueDto> dictionary, HashMap<String, LinkedList<Integer>> finalHeaderToLine,
                    Map<String, String> nullsField) {
        List<PropertyValueDto> propertiesValue = new ArrayList<>();

        HashMap<String, String> line = getLineFromOriginalLine(originalLine, finalHeaderToLine, dictionary);

        if (columnToRead >= 0) {
            if (columnToRead >= originalLine.length) {
                log.warn("Skipping value for column index {} because line has only {} columns", columnToRead, originalLine.length);
            } else {
                String headerName = getKeyByValue(columnToRead, finalHeaderToLine);
                String raw = originalLine[columnToRead];

                propertiesValue.add(PropertyValueDto.builder()
                        .name(headerName)
                        .val(MappingUtils.getCorrectValue(headerName, raw, dictionary))
                        .build());

                propertiesWithoutSubs.forEach(property -> {
                    if (!property.equalsIgnoreCase(headerName)) {
                        MappingUtils.addValueToPropertyList(property, finalHeaderToLine, columnToRead, line, propertiesValue, dictionary);
                    }
                });
            }
        } else {
            propertiesWithoutSubs.forEach(property -> {
                MappingUtils.addValueToPropertyList(property, finalHeaderToLine, columnToRead, line, propertiesValue, dictionary);
            });
        }

        MappingUtils.addNullFields(propertiesValue, nullsField);

        return propertiesValue;
    }

    private String getKeyByValue(int findValue, HashMap<String, LinkedList<Integer>> finalHeaderToLine) {

        String[] header = finalHeaderToLine.keySet().toArray(new String[0]);

        for (String name : header) {
            if (finalHeaderToLine.get(name).contains(findValue)) {
                return name;
            }
        }

        throw new IndexOutOfBoundsException("Non ho trovato l'header!");

    }

    private HashMap<String, String> getLineFromOriginalLine(String[] originalLine, HashMap<String, LinkedList<Integer>> finalHeaderToLine,
                                             List<ValueDto> dictionary) {

        HashMap<String, String> resultMap = new HashMap<>();

        List<String> withoutNegativeHeader = dictionary.stream()
                .filter(dto -> dto.getNameForNegative() == null || dto.getNameForNegative().isBlank())
                .map(ValueDto::getName)
                .toList();

        List<String> withNegativeHeader = dictionary.stream()
                .filter(dto -> dto.getNameForNegative() != null && !dto.getNameForNegative().isBlank())
                .map(ValueDto::getName)
                .toList();

        List<String> negativeHeader = dictionary.stream()
                .filter(dto -> dto.getNameForNegative() != null && !dto.getNameForNegative().isBlank())
                .map(ValueDto::getNameForNegative)
                .toList();

        finalHeaderToLine.forEach((key, item) -> {
            item.forEach(value -> {
                if (value >= originalLine.length) {
                    log.warn("Skipping column '{}' at index {} because line has only {} columns", key, value, originalLine.length);
                    return;
                }
                if (withoutNegativeHeader.contains(key)) {
                    resultMap.put(key, originalLine[value]);
                } else if (withNegativeHeader.contains(key)) {
                    resultMap.put(key, originalLine[value].charAt(0) != '-' ? originalLine[value] : "null");
                } else if (negativeHeader.contains(key)) {
                    resultMap.put(key, originalLine[value].charAt(0) == '-' ? originalLine[value] : "null");
                }
            });
        });

        return resultMap;
    }

    private PropertyDto retrievePropertyFromFilter(ExtractionDto retrievePropertyDto) {

        PropertyFilterDto propertyFilterDto = PropertyFilterDto.builder()
                .propertiesName(List.of(retrievePropertyDto.getPropertyName()))
                .build();

        return propertyService.getFilteredProperties(propertyFilterDto).getFirst();
    }

    private int resolveSliceMinutes(PropertyDto propertyDto) {
        if (propertyDto == null || propertyDto.getConfigurations() == null) {
            return -1;
        }
        Object rawSlice = propertyDto.getConfigurations().get("slice");
        if (rawSlice instanceof Number number) {
            return number.intValue();
        }
        if (rawSlice instanceof String str && !str.isBlank()) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                log.warn("Unable to parse slice configuration '{}' for property {}", str, propertyDto.getName());
            }
        }
        return -1;
    }

    private void enforceSliceDurationAfterNormalization(List<ResultValueDto> values, int sliceMinutes) {
        if (values == null || sliceMinutes <= 0) {
            return;
        }
        for (ResultValueDto value : values) {
            HashMap<String, Object> period = value.getPeriod();
            if (period == null) {
                continue;
            }
            Object startObj = period.get("start_ts");
            if (startObj == null) {
                continue;
            }
            String updatedEnd = TimeUtils.addMinutesToTimestamp(startObj.toString(), sliceMinutes);
            if (updatedEnd != null) {
                period.put("end_ts", updatedEnd);
            }
        }
    }





    private void writeJsonFile(ResultUrbanDataset urbanDataset, ExtractionDto extractionDto, String inputFilePath, String fileName) throws IOException {
        // Determine the UTC offset for the target UD. Fallback to UTC if none provided.
        String udOffsetStr = extractionDto.getUdUtc() != null && !extractionDto.getUdUtc().isBlank() ? extractionDto.getUdUtc() : "0";
        ZoneOffset udOffset = TimeUtils.parseUtcOffset(udOffsetStr);

        // Build a base timestamp for the context and file names using the target offset.
        java.time.Instant nowInstant = java.time.Instant.now();
        // Context timestamp must follow pattern yyyyMMddHHmmss (AAAAMMGGHHMMSS)
        String contextTimestamp = TimeUtils.formatAaaaMmGgHhMmSs(nowInstant, udOffset);

        // Set context timeZone and timestamp on the UD context
        if (urbanDataset != null && urbanDataset.getUrbanDataset() != null
                && urbanDataset.getUrbanDataset().getContext() != null) {
            // Override the time zone with the extraction config if present
            if (extractionDto.getUdUtc() != null && !extractionDto.getUdUtc().isBlank()) {
                urbanDataset.getUrbanDataset().getContext().setTimeZone(extractionDto.getUdUtc());
            }
            urbanDataset.getUrbanDataset().getContext().setTimestamp(contextTimestamp);
        }

        // Determine the safe resourceId from the context producer id
        String resourceId = "unknown-resource";
        if (urbanDataset != null
            && urbanDataset.getUrbanDataset() != null
            && urbanDataset.getUrbanDataset().getContext() != null
            && urbanDataset.getUrbanDataset().getContext().getProducer() != null
            && urbanDataset.getUrbanDataset().getContext().getProducer().getId() != null) {
                resourceId = urbanDataset.getUrbanDataset().getContext().getProducer().getId();
        }
        String safeResourceId = (resourceId == null || resourceId.isBlank())
            ? "unknown-resource"
            : resourceId.replaceAll("[\\\\/:*?\"<>|]", "-").trim();

        // Retrieve all lines
        List<ResultValueDto> lines = new ArrayList<>();
        if (urbanDataset != null
                && urbanDataset.getUrbanDataset() != null
                && urbanDataset.getUrbanDataset().getValues() != null
                && urbanDataset.getUrbanDataset().getValues().getLine() != null) {
            lines = urbanDataset.getUrbanDataset().getValues().getLine();
        }
        int totalLines = lines != null ? lines.size() : 0;
        log.debug("Transformer Service: preparing to write {} total rows", totalLines);
        // Determine number of parts based on maxRowsPerUd
        int parts = 1;
        if (maxRowsPerUd > 0 && totalLines > maxRowsPerUd) {
            parts = (int) Math.ceil(totalLines / (double) maxRowsPerUd);
        }
        int startIndex = 0;
        for (int part = 1; part <= parts; part++) {
            int endIndex;
            if (maxRowsPerUd > 0) {
                endIndex = Math.min(startIndex + maxRowsPerUd, totalLines);
            } else {
                endIndex = totalLines;
            }
            // Build a new UrbanDatasetDto using the same specification and context but sliced values
            UrbanDatasetDto partUd = new UrbanDatasetDto();
            partUd.setSpecification(urbanDataset.getUrbanDataset().getSpecification());
            partUd.setContext(urbanDataset.getUrbanDataset().getContext());
            ValuesDto partValues = new ValuesDto();
            if (lines != null) {
                partValues.setLine(lines.subList(startIndex, endIndex));
            } else {
                partValues.setLine(new ArrayList<>());
            }
            partUd.setValues(partValues);
            ResultUrbanDataset partResult = ResultUrbanDataset.builder().urbanDataset(partUd).build();
            // Compose file name: [resourceId]_-_[timestamp] + optional suffix
            String baseName = "[" + safeResourceId + "]_-_[" + contextTimestamp + "]";
            String outName;
            if (parts > 1) {
                outName = baseName + "_" + part + ".json";
            } else {
                outName = baseName + ".json";
            }
            String newFilePath = extractionDto.getOutputFilesPath() + "/" + outName;
            log.debug("Transformer Service: writing part {}/{} to {}", part, parts, newFilePath);
            FileUtils.createFile(newFilePath, partResult, objectMapper);
            startIndex = endIndex;
        }
        // Move the processed CSV to the completed folder only once
        String completedPath = extractionDto.getSourceFilesPath() + COMPLETED_PATH + fileName;
        log.debug("Transformer Service: moving source file to {}", completedPath);
        FileUtils.moveFile(inputFilePath, completedPath);
        log.debug("Transformer Service: completed processing for {}", fileName);
    }


    private void convertedHeaderFromMappings(String[] originalHeader, HashMap<String, ValueDto> dictionary, HashMap<String, LinkedList<Integer>> finalHeaderToLine) {

        for (int i = 0; i < originalHeader.length; i++) {
            if (dictionary.containsKey(originalHeader[i])) {

                addIndexToFinalHeader(finalHeaderToLine, dictionary.get(originalHeader[i]).getName(), i);

                if (dictionary.get(originalHeader[i]).getNameForNegative() != null){
                    LinkedList<Integer> listIndex = new LinkedList<>();
                    listIndex.add(i);
                    finalHeaderToLine.put(dictionary.get(originalHeader[i]).getNameForNegative(), listIndex);
                }
                    
            }
        }

    }




    
    private void addIndexToFinalHeader(HashMap<String, LinkedList<Integer>> finalHeader, String key, int index) {
    
        if (finalHeader.containsKey(key)) {
            finalHeader.get(key).add(index);
        } else {
            LinkedList<Integer> list = new LinkedList<>();
            list.add(index);
            finalHeader.put(key, list);
        }

    }

    private List<Integer> retrieveColumnsNumberForHeader(List<String> columns, String[] header) {
        List<Integer> result = new ArrayList<>();

        if (columns == null)
            return result;

        for(String string : columns) {
            for (int j=0; j < header.length; j++) {
                if (string.equalsIgnoreCase(header[j])) {
                    result.add(j);
                }
            }
        }

        return result;

    }

    private int retrieveColumnDataFromPeriod(String column, String[] header) {

        for (int i=0; i<header.length; i++) {
            if (header[i].equalsIgnoreCase(column)) {
                return i;
            }
        }

        return -1;
    }

}
