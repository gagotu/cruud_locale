package com.exprivia.nest.cruud.controller;

import com.exprivia.nest.cruud.dto.JSONDto;
import com.exprivia.nest.cruud.dto.RequestResultDto;
import com.exprivia.nest.cruud.dto.ResultUrbanDataset;
import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.service.ExternalService;
import com.exprivia.nest.cruud.service.TransformerService;
import com.exprivia.nest.cruud.utils.Endpoints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Transformer Controller that expose REST API for convert CSV and JSON retrieved
 */
@Slf4j
@RestController
@RequestMapping(Endpoints.TRANSFORMER)
public class TransformerController {

    @Autowired
    private TransformerService transformerService;

    @Autowired
    private ExternalService externalService;

    /**
     * Method to convert a csv into transformed UD OWL JSON
     *
     * @return operation result
     */
    @PostMapping(Endpoints.CSV)
    public RequestResultDto executeConversionFromFolder(@RequestBody ExtractionDto extractionDto) {
        log.debug("Transformer Controller: csvTransformed -> {}", extractionDto);
        return transformerService.executeConversionFromFolder(extractionDto);
    }

    /**
     * Method to execute csv conversion from extraction data retrieved by extractionName
     *
     * @param extractionName name of extraction
     *
     * @return operation result
     */
    @GetMapping(Endpoints.CSV + Endpoints.SLASH + "{extractionName}")
    public RequestResultDto executeConversionFromExtraction(@PathVariable String extractionName) {
        log.debug("Transformer controller: csvTransformedByExtraction -> {}", extractionName);
        return transformerService.executeConversionFromExtraction(extractionName);
    }

    /**
     * Method to convert a file uploaded into UD OWL JSON
     *
     * @param file to convert
     * @return urban dataset
     */
    @PostMapping(Endpoints.UPLOAD + Endpoints.SLASH + "{property}")
    public ResultUrbanDataset executeConversionFromUpload(@RequestParam("file") MultipartFile file, @PathVariable String property) {
        log.debug("Transformer controller: uploadCsvTransformed");
        return transformerService.executeConversionFromUpload(file, property);
    }

    /**
     * Method to convert a JSON retrieved from open cruise platform
     *
     * @param jsonDto to convert
     * @param extractionName to use
     * @return urban dataset
     */
    @PostMapping(Endpoints.EXTERNAL + Endpoints.OPEN_CRUISE + Endpoints.SLASH + "{extractionName}")
    public ResultUrbanDataset executeConversionFromOpenCruise(@RequestBody JSONDto jsonDto, @PathVariable String extractionName) {
        log.debug("External controller: getFromOpenCruise -> {} - {}", jsonDto, extractionName);
        return externalService.executeConversionFromOpenCruise(jsonDto, extractionName);
    }

}