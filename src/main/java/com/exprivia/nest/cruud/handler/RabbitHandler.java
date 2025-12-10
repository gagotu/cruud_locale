package com.exprivia.nest.cruud.handler;

import com.exprivia.nest.cruud.dto.ExtractionDto;
import com.exprivia.nest.cruud.service.ExternalService;
import com.exprivia.nest.cruud.service.ExtractionService;
import com.exprivia.nest.cruud.service.TransformerService;
import com.exprivia.nest.cruud.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Rabbit Handler, used to listen into queues
 */
@Slf4j
@Component
public class RabbitHandler {

    @Autowired
    private ExtractionService extractionService;

    @Autowired
    private ExternalService externalService;

    @Autowired
    private TransformerService transformerService;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Method that consume message from convert csv queue
     */
    @RabbitListener(queues = "${spring.rabbitmq.queues.convert_csv}")
    public void consumeConvertCsv(String msg) {
        log.debug("Message convert received. {}", msg);
        log.debug("Start convert from csv");

        List<ExtractionDto> extractions = extractionService.getAll().stream()
                .filter(extractionDto -> extractionDto.getAutoConvert() == Boolean.TRUE)
                .toList();

        extractions.forEach(object -> {
            log.info("execute event transform for: {}", object.getPropertyName());

            if (object.getSourceFilesPath() != null) {
                transformerService.executeConversionFromFolder(object);
            }

            if (object.getSourceRestApi() != null) {
                //TODO: After integration, remove JSONDto parameter. And add getSourceRestApi. end remove under comment
                /*ResultUrbanDataset resultUrbanDataset = externalService.executeConversionFromOpenCruise(JSONDto.builder().build(), object.getPropertyName());
                try {
                    SimpleDateFormat formatter = new SimpleDateFormat("ddMMyyyy");
                    String formattedDate = formatter.format(new Date());
                    Utils.createFile(object.getPropertyName() + "_" + formattedDate + "_" + UUID.randomUUID(), resultUrbanDataset, objectMapper);
                } catch (IOException e) {
                    log.error("Errore durante la creazione del file JSON!", e);
                    throw new RuntimeException(e);
                }*/
            }
        });
    }

    /**
     * Method that consume message from clean csv queue
     */
    @RabbitListener(queues = "${spring.rabbitmq.queues.clean_csv}")
    public void consumeCleanCsv(String msg) {
        log.debug("Message clean received. {}", msg);
        log.debug("Start clean for completed csv...");

        List<String> pathToClean = extractionService.getAll().stream()
                .filter(extractionDto -> extractionDto.getAutoClean() == Boolean.TRUE)
                .map(ExtractionDto::getSourceFilesPath)
                .distinct()
                .toList();

        pathToClean.forEach(path -> {
            log.info("execute event clean for completed folder into: {}", path);
            Utils.cleanFilesCompleted(path + "/completed/");
        });
    }

}
