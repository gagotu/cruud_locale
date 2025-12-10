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
import com.exprivia.nest.cruud.utils.Utils;
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
import java.time.ZoneOffset;
import java.util.*;

/**
 * Transformer Service that expose business methods to convert data into UD OWL
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

        List<String> filesRetrieved = Utils.getFilesNameFromPath(extractionDto.getSourceFilesPath());
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

        Utils.cleanFilesCompleted(tempFolderPath); //remove temporary file used.

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

        Utils.updateSpecificationAndContext(specificationDto, contextDto, extractionDto);

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
            if (udUtcStr != null && !udUtcStr.isBlank()) {
                int originalSize = values != null ? values.size() : 0;
                values = timeNormalizationService.normalizePeriods(values, extractionDto.getTimeZone(), udUtcStr, handleDst);
                log.debug("Transformer Service: periods normalized (kept {} of {})", values != null ? values.size() : 0, originalSize);
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

        int dateColumn = -1;
        if (configurations.containsKey("date")) {
            dateColumn = retrieveColumnsNumberForHeader(List.of(configurations.get("date").toString()), originalHeader).getFirst();
        }

        Map<String, String> nullsFieldMap = Utils.toStringMap(configurations.get("nullsField"));

        int id = 1;
        HashMap<String, Object> period;
        while ((line = reader.readNext()) != null) {
            List<String> periods = (List<String>) configurations.get("period");
            List<Integer> columnsPeriods = retrieveColumnsNumberForHeader(periods, originalHeader);

            if (!columnsPeriods.isEmpty()) {
                for (int i=0; i<columnsPeriods.size(); i++) {

                    if (line[dateColumn] != null && !line[dateColumn].isBlank()) {
                        //Effettua le operazioni solo se il datetime è valorizzato
                        ResultValueDto resultValueDto = ResultValueDto.builder().id(id).build();

                        int slice = (int) configurations.getOrDefault("slice", -1);

                        period = Utils.getStartAndEndTimeFromString(line[dateColumn], originalHeader[columnsPeriods.get(i)], slice);

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

        reader.close(); //close reader csv
        fileReader.close(); //close file

        return values;
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

    private List<PropertyValueDto> processPropertiesValue(String[] originalLine, int columnToRead,
                    List<String> propertiesWithoutSubs, List<ValueDto> dictionary, HashMap<String, LinkedList<Integer>> finalHeaderToLine,
                    Map<String, String> nullsField) {
        List<PropertyValueDto> propertiesValue = new ArrayList<>();

        HashMap<String, String> line = getLineFromOriginalLine(originalLine, finalHeaderToLine, dictionary);

        if (columnToRead >= 0) {
            String headerName = getKeyByValue(columnToRead, finalHeaderToLine);

            propertiesValue.add(PropertyValueDto.builder()
                    .name(headerName)
                    .val(Utils.getCorrectValue(headerName, originalLine[columnToRead], dictionary))
                    .build());

            propertiesWithoutSubs.forEach(property -> {
                if (!property.equalsIgnoreCase(headerName)) {
                    Utils.addValueToPropertyList(property, finalHeaderToLine, columnToRead, line, propertiesValue, dictionary);
                }
            });
        } else {
            propertiesWithoutSubs.forEach(property -> {
                Utils.addValueToPropertyList(property, finalHeaderToLine, columnToRead, line, propertiesValue, dictionary);
            });
        }

        Utils.addNullFields(propertiesValue, nullsField);

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





    private void writeJsonFile(ResultUrbanDataset urbanDataset, ExtractionDto extractionDto, String inputFilePath, String fileName) throws IOException {
        // Determine the UTC offset for the target UD. Fallback to UTC if none provided.
        String udOffsetStr = extractionDto.getUdUtc() != null && !extractionDto.getUdUtc().isBlank() ? extractionDto.getUdUtc() : "0";
        ZoneOffset udOffset = Utils.parseUtcOffset(udOffsetStr);

        // Build a base timestamp for the context and file names using the target offset.
        java.time.Instant nowInstant = java.time.Instant.now();
        // Context timestamp must follow pattern yyyyMMddHHmmss (AAAAMMGGHHMMSS)
        String contextTimestamp = Utils.formatAaaaMmGgHhMmSs(nowInstant, udOffset);

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
            Utils.createFile(newFilePath, partResult, objectMapper);
            startIndex = endIndex;
        }
        // Move the processed CSV to the completed folder only once
        String completedPath = extractionDto.getSourceFilesPath() + COMPLETED_PATH + fileName;
        log.debug("Transformer Service: moving source file to {}", completedPath);
        Utils.moveFile(inputFilePath, completedPath);
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
