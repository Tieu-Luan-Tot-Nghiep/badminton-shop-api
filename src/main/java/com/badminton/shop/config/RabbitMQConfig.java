package com.badminton.shop.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EMAIL_EXCHANGE = "email.exchange";
    public static final String EMAIL_VERIFICATION_QUEUE = "email.verification.queue";
    public static final String EMAIL_VERIFICATION_ROUTING_KEY = "email.verification.routingKey";
    public static final String AVATAR_UPDATE_QUEUE = "avatar.update.queue";
    public static final String AVATAR_UPDATE_ROUTING_KEY = "avatar.update.routingKey";

    @Bean
    public Queue emailVerificationQueue() {
        return new Queue(EMAIL_VERIFICATION_QUEUE, true);
    }

    @Bean
    public Queue avatarUpdateQueue() {
        return new Queue(AVATAR_UPDATE_QUEUE, true);
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public Binding emailBinding(Queue emailVerificationQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(emailVerificationQueue).to(emailExchange).with(EMAIL_VERIFICATION_ROUTING_KEY);
    }

    @Bean
    public Binding avatarUpdateBinding(Queue avatarUpdateQueue, DirectExchange emailExchange) {
        return BindingBuilder.bind(avatarUpdateQueue).to(emailExchange).with(AVATAR_UPDATE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
