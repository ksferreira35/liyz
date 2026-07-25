package br.com.kaiky.todo_list.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationRabbitConfig {

    @Bean
    public DirectExchange taskNotificationExchange(
            @Value("${app.notifications.rabbit.exchange}") String exchange
    ) {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    public Queue taskNotificationQueue(
            @Value("${app.notifications.rabbit.queue}") String queue
    ) {
        return new Queue(queue, true);
    }

    @Bean
    public Binding taskNotificationBinding(
            DirectExchange taskNotificationExchange,
            Queue taskNotificationQueue,
            @Value("${app.notifications.rabbit.routing-key}") String routingKey
    ) {
        return BindingBuilder
                .bind(taskNotificationQueue)
                .to(taskNotificationExchange)
                .with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

}
