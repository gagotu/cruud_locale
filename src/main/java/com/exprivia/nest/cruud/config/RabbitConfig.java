package com.exprivia.nest.cruud.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione delle code RabbitMQ usate per le richieste di conversione/cleaning.
 * Qui definiamo i nomi delle code e le esponiamo come bean Spring.
 */
@Configuration
public class RabbitConfig {

    @Value("${spring.rabbitmq.queues.convert_csv}")
    private String convert_csv_queue;

    @Value("${spring.rabbitmq.queues.clean_csv}")
    private String clean_csv_queue;

    /**
     * Coda per le richieste di conversione CSV→UD.
     */
    @Bean
    public Queue createQueueConvertCsv() {
        return new Queue(convert_csv_queue, true);
    }

    /**
     * Coda per le richieste di pulizia CSV.
     */
    @Bean
    public Queue createQueueCleanCsv() {
        return new Queue(clean_csv_queue, true);
    }

}
