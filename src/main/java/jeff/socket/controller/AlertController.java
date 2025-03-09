package jeff.socket.controller;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import jeff.socket.dto.Alert;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Controller
public class AlertController {
    
    @MessageMapping("alert")
    public Mono<Void> setAlert(Mono<Alert> alertMono){
        return alertMono
                .doOnNext(alert -> log.info("{} ordered by {} at {}",alert.getLevel(),alert.getOrderedBy(),alert.getOrderedAt()))
                .thenEmpty(Mono.empty());
    }

}
