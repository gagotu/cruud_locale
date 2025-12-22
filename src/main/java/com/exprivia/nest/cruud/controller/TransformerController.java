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
 * Espone le API REST per trasformare CSV/JSON in UrbanDataset: conversione da
 * cartella, da nome di estrazione, upload diretto e sorgenti esterne (OpenCruise).
 */
@Slf4j
@RestController
@RequestMapping(Endpoints.TRANSFORMER)
public class TransformerController {

    @Autowired
    private TransformerService transformerService;

    @Autowired
    private ExternalService externalService;

    /** Converte i CSV presenti in una cartella secondo la configurazione ricevuta. */
    @PostMapping(Endpoints.CSV)
    public RequestResultDto executeConversionFromFolder(@RequestBody ExtractionDto extractionDto) {
        log.debug("Transformer Controller: csvTransformed -> {}", extractionDto);
        return transformerService.executeConversionFromFolder(extractionDto);
    }

    /** Converte usando la configurazione identificata dal nome di estrazione. */
    @GetMapping(Endpoints.CSV + Endpoints.SLASH + "{extractionName}")
    public RequestResultDto executeConversionFromExtraction(@PathVariable String extractionName) {
        log.debug("Transformer controller: csvTransformedByExtraction -> {}", extractionName);
        return transformerService.executeConversionFromExtraction(extractionName);
    }

    /** Converte un file CSV caricato via upload. */
    @PostMapping(Endpoints.UPLOAD + Endpoints.SLASH + "{property}")
    public ResultUrbanDataset executeConversionFromUpload(@RequestParam("file") MultipartFile file, @PathVariable String property) {
        log.debug("Transformer controller: uploadCsvTransformed");
        return transformerService.executeConversionFromUpload(file, property);
    }

    /** Converte un JSON proveniente da OpenCruise usando la relativa estrazione. */
    @PostMapping(Endpoints.EXTERNAL + Endpoints.OPEN_CRUISE + Endpoints.SLASH + "{extractionName}")
    public ResultUrbanDataset executeConversionFromOpenCruise(@RequestBody JSONDto jsonDto, @PathVariable String extractionName) {
        log.debug("External controller: getFromOpenCruise -> {} - {}", jsonDto, extractionName);
        return externalService.executeConversionFromOpenCruise(jsonDto, extractionName);
    }

}
