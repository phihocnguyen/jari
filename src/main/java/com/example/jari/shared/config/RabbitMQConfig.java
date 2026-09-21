package com.example.jari.shared.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EVENTS_EXCHANGE = "jari.events";

    public static final String NOTIFICATION_QUEUE   = "jari.notifications.queue";
    public static final String NOTIFICATION_ROUTING = "notification.#";

    public static final String INDEXER_QUEUE   = "jari.indexer.queue";
    public static final String INDEXER_ROUTING = "indexer.#";

    public static final String RK_NOTIFICATION_USER = "notification.user";
    public static final String RK_INDEXER_ISSUE     = "indexer.issue";
    public static final String RK_INDEXER_PROJECT   = "indexer.project";
    public static final String RK_INDEXER_USER      = "indexer.user";

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Queue indexerQueue() {
        return QueueBuilder.durable(INDEXER_QUEUE).build();
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventsExchange).with(NOTIFICATION_ROUTING);
    }

    @Bean
    public Binding indexerBinding(Queue indexerQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(indexerQueue).to(eventsExchange).with(INDEXER_ROUTING);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(jsonMessageConverter);
        return template;
    }
}
