package net.hwyz.iov.cloud.iov.ota.service.infrastructure.event.publisher;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.ota.service.domain.model.event.TaskEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(TaskEvent event) {
        applicationEventPublisher.publishEvent(event);
    }

    public void publishAll(List<TaskEvent> events) {
        events.forEach(this::publish);
    }
}