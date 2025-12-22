package com.exprivia.nest.cruud.config;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configurazione AMQP di base: definisce il converter usato da Rabbit per
 * serializzare/deserializzare i messaggi verso i DTO del progetto.
 */
@Configuration
public class AmqpConfig {

    /**
     * Converter permesso per i messaggi che veicolano i DTO (ExtractionDto).
     * Limitiamo i pattern di classe consentiti per evitare serializzazioni non attese.
     */
    @Bean
    public MessageConverter messageConverter() {
        SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();
        simpleMessageConverter.addAllowedListPatterns("com.exprivia.nest.cruud.dto.ExtractionDto");
        return simpleMessageConverter;
    }

}
