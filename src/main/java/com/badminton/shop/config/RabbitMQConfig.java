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
    public static final String CART_EXCHANGE = "cart.exchange";
    public static final String CART_SYNC_QUEUE = "cart.sync.queue";
    public static final String CART_SYNC_ROUTING_KEY = "cart.sync.routingKey";
    public static final String PROMOTION_EXCHANGE = "promotion.exchange";
    public static final String PROMOTION_USAGE_QUEUE = "promotion.usage.queue";
    public static final String PROMOTION_USAGE_ROUTING_KEY = "promotion.usage.routingKey";
    public static final String INVENTORY_EXCHANGE = "inventory.exchange";
    public static final String INVENTORY_COMMAND_QUEUE = "inventory.command.queue";
    public static final String INVENTORY_COMMAND_ROUTING_KEY = "inventory.command.routingKey";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CANCELLED_QUEUE = "order.cancelled.queue";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled.routingKey";
    public static final String REFUND_REQUIRED_QUEUE = "order.refund-required.queue";
    public static final String REFUND_REQUIRED_ROUTING_KEY = "order.refund-required.routingKey";
    public static final String CHAT_EXCHANGE = "chat.exchange";
    public static final String CHAT_MESSAGE_PERSIST_QUEUE = "chat.message.persist.queue";
    public static final String CHAT_MESSAGE_PERSIST_ROUTING_KEY = "chat.message.persist.routingKey";

    @Bean
    public Queue emailVerificationQueue() {
        return new Queue(EMAIL_VERIFICATION_QUEUE, true);
    }

    @Bean
    public Queue avatarUpdateQueue() {
        return new Queue(AVATAR_UPDATE_QUEUE, true);
    }

    @Bean
    public Queue cartSyncQueue() {
        return new Queue(CART_SYNC_QUEUE, true);
    }

    @Bean
    public Queue promotionUsageQueue() {
        return new Queue(PROMOTION_USAGE_QUEUE, true);
    }

    @Bean
    public Queue inventoryCommandQueue() {
        return new Queue(INVENTORY_COMMAND_QUEUE, true);
    }

    @Bean
    public Queue orderCancelledQueue() {
        return new Queue(ORDER_CANCELLED_QUEUE, true);
    }

    @Bean
    public Queue refundRequiredQueue() {
        return new Queue(REFUND_REQUIRED_QUEUE, true);
    }

    @Bean
    public Queue chatMessagePersistQueue() {
        return new Queue(CHAT_MESSAGE_PERSIST_QUEUE, true);
    }

    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE);
    }

    @Bean
    public DirectExchange cartExchange() {
        return new DirectExchange(CART_EXCHANGE);
    }

    @Bean
    public DirectExchange promotionExchange() {
        return new DirectExchange(PROMOTION_EXCHANGE);
    }

    @Bean
    public DirectExchange inventoryExchange() {
        return new DirectExchange(INVENTORY_EXCHANGE);
    }

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    @Bean
    public DirectExchange chatExchange() {
        return new DirectExchange(CHAT_EXCHANGE);
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
    public Binding cartSyncBinding(Queue cartSyncQueue, DirectExchange cartExchange) {
        return BindingBuilder.bind(cartSyncQueue).to(cartExchange).with(CART_SYNC_ROUTING_KEY);
    }

    @Bean
    public Binding promotionUsageBinding(Queue promotionUsageQueue, DirectExchange promotionExchange) {
        return BindingBuilder.bind(promotionUsageQueue).to(promotionExchange).with(PROMOTION_USAGE_ROUTING_KEY);
    }

    @Bean
    public Binding inventoryCommandBinding(Queue inventoryCommandQueue, DirectExchange inventoryExchange) {
        return BindingBuilder.bind(inventoryCommandQueue).to(inventoryExchange).with(INVENTORY_COMMAND_ROUTING_KEY);
    }

    @Bean
    public Binding orderCancelledBinding(Queue orderCancelledQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderCancelledQueue).to(orderExchange).with(ORDER_CANCELLED_ROUTING_KEY);
    }

    @Bean
    public Binding refundRequiredBinding(Queue refundRequiredQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(refundRequiredQueue).to(orderExchange).with(REFUND_REQUIRED_ROUTING_KEY);
    }

    @Bean
    public Binding chatMessagePersistBinding(Queue chatMessagePersistQueue, DirectExchange chatExchange) {
        return BindingBuilder.bind(chatMessagePersistQueue).to(chatExchange).with(CHAT_MESSAGE_PERSIST_ROUTING_KEY);
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
