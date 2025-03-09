package jeff.socket;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.rsocket.RSocketRequester;

import jeff.socket.dto.Alert;
import jeff.socket.dto.GratuityIn;
import jeff.socket.dto.GratuityOut;
import jeff.socket.dto.StockQuote;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;


@Slf4j
@SpringBootApplication
public class SocketApplication {

	public static void main(String[] args) {
		SpringApplication.run(SocketApplication.class, args);
	}

	@Bean
	public ApplicationRunner sender(RSocketRequester.Builder requesterBuilder){
		return args -> {
			RSocketRequester tcp = requesterBuilder.tcp("localhost", 7000);

			//Request-Response client
			tcp.route("greeting")
				.data("Hello Rsocket!")
				.retrieveMono(String.class)
				.subscribe(response -> log.info("Got a response: {}",response));

			tcp.route("greeting/{name}", "Jeff")
				.data("Hello Rsocket!")
				.retrieveMono(String.class)
				.subscribe(response -> log.info("Got a response: {}",response));

			//Request-Stream client
			tcp.route("stock/{symbol}","xyz")
				.retrieveFlux(StockQuote.class)
				.doOnNext(stockQuote -> log.info("Price of {}: {} at {}",stockQuote.getSymbol(),stockQuote.getPrice(),stockQuote.getPrice()))
				.subscribe();

			//Fire and Forget client
			tcp.route("alert")
				.data(new Alert(Alert.Level.RED,"C-3PO",Instant.now()))
				.send() // instead of retrieveMono or retrieveFlux
				.subscribe();
			log.info("Alert sent!");

			Flux<GratuityIn> gratuityInFlux = Flux.fromArray(
				new GratuityIn[]{
					new GratuityIn(BigDecimal.valueOf(35.50),18),
					new GratuityIn(BigDecimal.valueOf(10.25),18),
					new GratuityIn(BigDecimal.valueOf(80.12),20),
					new GratuityIn(BigDecimal.valueOf(100.89),30),
					new GratuityIn(BigDecimal.valueOf(62.50),40)
				}
			)
			.delayElements(Duration.ofSeconds(1));

			// Request channel client
			tcp.route("gratuity")
				.data(gratuityInFlux)
				.retrieveFlux(GratuityOut.class)
				.subscribe(out -> log.info("{}% gratuity on {} is {}",out.getPercent(),out.getBillTotal(),out.getGratuity()));
		};
	}

}
