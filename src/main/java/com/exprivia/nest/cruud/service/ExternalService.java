package com.exprivia.nest.cruud.service;

import com.exprivia.nest.cruud.dto.*;
import com.exprivia.nest.cruud.dto.sourcedataset.DynamicValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.UrbanDatasetDto;
import com.exprivia.nest.cruud.dto.urbandataset.context.ContextDto;
import com.exprivia.nest.cruud.dto.urbandataset.specification.SpecificationDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.PropertyValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.ResultValueDto;
import com.exprivia.nest.cruud.dto.urbandataset.values.ValuesDto;
import com.exprivia.nest.cruud.utils.FileUtils;
import com.exprivia.nest.cruud.utils.MappingUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Servizio che chiama piattaforme esterne (es. OpenCruise), recupera JSON
 * di input e li trasforma in UrbanDataset usando le property/estrazioni configurate.
 */
@Slf4j
@Service
public class ExternalService {

    @Autowired
    private PropertyService propertyService;

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${temp-folder}")
    private String tempFolder;

    /**
     * Execute conversion from open cruise
     *
     * @param jsonDto json to convert
     * @param extractionName to use
     * @return urban dataset
     */
    public ResultUrbanDataset executeConversionFromOpenCruise(JSONDto jsonDto, String extractionName) {
        log.debug("External Service: getFromOpenCruise -> {} - {}", jsonDto, extractionName);

        Optional<ExtractionDto> extractionDtoOptional = extractionService.getByExtractionName(extractionName);

        if (extractionDtoOptional.isPresent()) {
            ExtractionDto extractionDto = extractionDtoOptional.get();

            PropertyFilterDto propertyFilterDto = PropertyFilterDto.builder().propertiesName(List.of(extractionDto.getPropertyName())).build();

            List<PropertyDto> properties = propertyService.getFilteredProperties(propertyFilterDto);
            if (properties.isEmpty()) {
                log.error("Property not found for name {}", extractionDto.getPropertyName());
                return null;
            }
            PropertyDto propertyDto = properties.getFirst();

            ResultUrbanDataset resultUrbanDataset = null;

            // Start prepared UD

            SpecificationDto specificationDto;
            ContextDto contextDto;

            try {
                specificationDto = objectMapper.convertValue(propertyDto.getSpecification(), SpecificationDto.class);
                contextDto = objectMapper.convertValue(propertyDto.getContext(), ContextDto.class);
            } catch (IllegalArgumentException e) {
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

            urbanDataset.setValues(ValuesDto.builder().line(
                    retrieveValueFromJson(propertyDto, jsonDto) // retrieve single values data of UD
            ).build());
            resultUrbanDataset = ResultUrbanDataset.builder().urbanDataset(urbanDataset).build();
            // End values UD

            try {
                FileUtils.createFile(tempFolder + "json_example.json", resultUrbanDataset, objectMapper);
            } catch (IOException e) {
                log.error("Error during creation of JSON file result");
                throw new RuntimeException(e);
            }

            return resultUrbanDataset;
        }

        log.error("Extraction not found!");
        return null;
    }

    private List<ResultValueDto> retrieveValueFromJson(PropertyDto propertyDto, JSONDto jsonDto) {
        List<ResultValueDto> values = new ArrayList<>();

        int id = 1;

        for (DynamicValueDto dynamicValueDto : jsonDto.getResult()) {
            ResultValueDto resultValueDto = ResultValueDto.builder().id(id).build();

            if (dynamicValueDto.getAttributes().containsKey("timestamp"))
                resultValueDto.setTimestamp(dynamicValueDto.getAttributes().get("timestamp"));

            List<PropertyValueDto> properties = new ArrayList<>();

            for (String key : propertyDto.getMappings().keySet()) {
                properties.add(PropertyValueDto.builder()
                        .name(propertyDto.getMappings().get(key).getName())
                        .val(dynamicValueDto.getAttributes().get(key).toString()).build());
            }

            MappingUtils.addNullFields(properties, MappingUtils.toStringMap(
                    propertyDto.getConfigurations() != null ? propertyDto.getConfigurations().get("nullsField") : null
            ));

            resultValueDto.setProperty(properties);

            values.add(resultValueDto);
            id++;
        }

        return values;
    }

}
