package com.exprivia.nest.cruud.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Rabbit Config class, used to create a queues
 */
@Configuration
public class RabbitConfig {

    @Value("${spring.rabbitmq.queues.convert_csv}")
    private String convert_csv_queue;

    @Value("${spring.rabbitmq.queues.clean_csv}")
    private String clean_csv_queue;

    /**
     * Method that create a convert csv queue
     *
     * @return Queue created
     */
    @Bean
    public Queue createQueueConvertCsv() {
        return new Queue(convert_csv_queue, true);
    }

    /**
     * Method that create a clean csv queue
     *
     * @return Queue created
     */
    @Bean
    public Queue createQueueCleanCsv() {
        return new Queue(clean_csv_queue, true);
    }

}
