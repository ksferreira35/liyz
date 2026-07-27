package br.com.kaiky.todo_list.service;

import br.com.kaiky.todo_list.dto.TaskNotificationMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TaskNotificationPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final String exchange;
    private final String routingKey;

    public TaskNotificationPublisher(
            RabbitTemplate rabbitTemplate,
            MessageConverter jsonMessageConverter,
            @Value("${app.notifications.rabbit.exchange}") String exchange,
            @Value("${app.notifications.rabbit.routing-key}") String routingKey
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitTemplate.setMessageConverter(jsonMessageConverter);
        this.exchange = exchange;
        this.routingKey = routingKey;
    }

    public void publish(TaskNotificationMessage message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
