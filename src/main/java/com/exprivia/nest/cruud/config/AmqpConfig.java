package com.exprivia.nest.cruud.config;

import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Amqp Config class
 */
@Configuration
public class AmqpConfig {

    /**
     * Method that convert a serializable into project object
     *
     * @return MessageConverter
     */
    @Bean
    public MessageConverter messageConverter() {
        SimpleMessageConverter simpleMessageConverter = new SimpleMessageConverter();
        simpleMessageConverter.addAllowedListPatterns("com.exprivia.nest.cruud.dto.ExtractionDto");
        return simpleMessageConverter;
    }

}
