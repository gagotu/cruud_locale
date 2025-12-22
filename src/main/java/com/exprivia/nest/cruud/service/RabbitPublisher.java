package com.exprivia.nest.cruud.service;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Publisher periodico su RabbitMQ: invia messaggi alle code di conversione e
 * pulizia con una frequenza configurata.
 */
@Service
public class RabbitPublisher {

    @Value("${spring.rabbitmq.queues.convert_csv}")
    private String convert_csv_queue;

    @Value("${spring.rabbitmq.queues.clean_csv}")
    private String clean_csv_queue;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** Pubblica un messaggio sulla coda di conversione con la frequenza configurata. */
    @Scheduled(fixedDelayString = "${spring.rabbitmq.auto_convert}")
    public void publishMessageConvert() {
        rabbitTemplate.convertAndSend(convert_csv_queue, "Publish convert event...");
    }

    @Scheduled(fixedDelayString = "${spring.rabbitmq.auto_clean}")
    public void publishMessageClean() {
        rabbitTemplate.convertAndSend(clean_csv_queue, "Publish clean event...");
    }

}
