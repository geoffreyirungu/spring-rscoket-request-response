# READ ME
I did this project as part of the spring in action 6th edition book.

The main takeaways of this project centers around using RSocket for inter-application communication:
* Reactive network communication with RSocket
* Working with each of RSocket four communication models
* Transporting RScoket over WebSocket

# RSOCKET

## Overview
Rsocket project is used for inter-application communication using the request-response, request-stream, fire-and-forget and request-channel communication models.

## Definition of the communication models
Request-response: a single request message from the client is followed by a single response message from the server.

Request-Stream: a single request message from the client is followed by a stream of response messages from the server.

Fire-and-Forget: A single request message is sent by the client, but no response is expected from the server.

Request-Channel: A stream of request messages from the client is followed by a stream of response messages from the server.

## Project Setup

To create an Rsocket project using the communication models, follow these steps:

1. Add Spring boot rsocket starter:

        <dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-rsocket</artifactId>
		</dependency>

2. Define controllers and their respective methods:

    ## Request-Response controller example
        @Controller
        @Slf4j
        public class GreetingController {
            
            @MessageMapping("greeting")
            public Mono<String> handleGreeting(Mono<String> greetingMono){
                return greetingMono
                        .doOnNext(greeting -> log.info("Received greeting: {}",greeting))
                        .map(greeting -> "Hello back to you!");
            }

            @MessageMapping("greeting/{name}")
            public Mono<String> handleGreeting(@DestinationVariable("name") String name,Mono<String> greetingMono){
                return greetingMono
                        .doOnNext(greeting -> log.info("Received greeting: {}",greeting))
                        .map(greeting -> "Hello "+name);
            }

        }

    ## Request-Stream controller example
        @Controller
        public class StockQuoteController {
            
            @MessageMapping("stock/{symbol}")
            public Flux<StockQuote> getStockPrice(@DestinationVariable("symbol") String symbol){
                return Flux
                        .interval(Duration.ofSeconds(1))
                        .map(i -> {
                            BigDecimal price = BigDecimal.valueOf(Math.random() * 10);
                            return new StockQuote(symbol,price,Instant.now());
                        });
            }

        }

    ## Fire and Forget controller example
        @Controller
        public class AlertController {
            
            @MessageMapping("alert")
            public Mono<Void> setAlert(Mono<Alert> alertMono){
                return alertMono
                        .doOnNext(alert -> log.info("{} ordered by {} at {}",alert.getLevel(),alert.getOrderedBy(),alert.getOrderedAt()))
                        .thenEmpty(Mono.empty());
            }

        }

    ## Request channel controller example
        public class GratuityController {
        
            @MessageMapping("gratuity")
            public Flux<GratuityOut> calculate(Flux<GratuityIn> gratuityInFlux){
                return gratuityInFlux
                        .doOnNext(in -> log.info("Calculating gratuity: {}",in))
                        .map(in -> {
                            double percentAsDecimal = in.getPercent() / 100.0;
                            BigDecimal gratuity = in.getBillTotal().multiply(BigDecimal.valueOf(percentAsDecimal));
                            return new GratuityOut(in.getBillTotal(),in.getPercent(),gratuity);
                        });
            }

        }

3. Configure the RSocket server:

        spring:
            rsocket:
                server:
                    port: 7000

4. Create an RSocket client and send messages:

        @Bean
        public ApplicationRunner sender(RSocketRequester.Builder requesterBuilder){
            return args -> {
                RSocketRequester tcp = requesterBuilder.tcp("localhost", 7000);

                // Request-Response client examples
                tcp.route("greeting")
                    .data("Hello Rsocket!")
                    .retrieveMono(String.class)
                    .subscribe(response -> log.info("Got a response: {}",response));

                tcp.route("greeting/{name}", "Jeff")
                    .data("Hello Rsocket!")
                    .retrieveMono(String.class)
                    .subscribe(response -> log.info("Got a response: {}",response));

                // Request-Stream client example
                tcp.route("stock/{symbol}","xyz")
                    .retrieveFlux(StockQuote.class)
                    .doOnNext(stockQuote -> log.info("Price of {}: {} at {}",stockQuote.getSymbol(),stockQuote.getPrice(),stockQuote.getPrice()))
                    .subscribe();

                //Fire and Forget client example
                tcp.route("alert")
                    .data(new Alert(Alert.Level.RED,"C-3PO",Instant.now()))
                    .send() // instead of retrieveMono or retrieveFlux
                    .subscribe();

                // Request channel client example
                tcp.route("gratuity")
                    .data(gratuityInFlux)
                    .retrieveFlux(GratuityOut.class)
                    .subscribe(out -> log.info("{}% gratuity on {} is {}",out.getPercent(),out.getBillTotal(),out.getGratuity()));

            };
        }


## Transport RSocket over WebSocket
By default RSocket communicaiton takes place over TCP Socket. But in some cases TCP is not an option.
To switch from TCP transport to WebSocket transport, you need to make minor changes to the server and the client.

Below are the steps of switching:

1. Add WebFlux starter dependency:

    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webflux</artifactId>
	</dependency>

2. Specify you want to use web socket transport in the configuration by setting spring.rsocket.server.   transport. Also set the Http path that the Rsocket communication will take place on by setting spring.rsocket.server.mapping-path

    spring:
        rsocket:
            server:
                transport: websocket
                mapping-path: /rsocket

    NB: unlike tcp transport which communicates over a specific port, the websocket transport works over a specific http path. Thus there is no need to set spring.rsocket.server.port as with Rsocket over TCP.

3. On the client side rather than create a tcp based requester you create a websocket based requester by calling websocket() method on RSocketRequester.Builder:

    RSocketRequester requester = requesterBuilder.websocket(URI.create("ws://localhost:8080/rsocket"));
    requester.route("greeting")
				.data("Hello Rsocket!")
				.retrieveMono(String.class)
				.subscribe(response -> log.info("Got a response: {}",response));