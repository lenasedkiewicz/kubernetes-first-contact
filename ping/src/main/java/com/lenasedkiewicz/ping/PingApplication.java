package com.lenasedkiewicz.ping;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@SpringBootApplication
@RestController
public class PingApplication {

    private WebClient webClient = WebClient.create();

	public static void main(String[] args) {
		SpringApplication.run(PingApplication.class, args);
	}

    // tag::ping-endpoint[]
    @GetMapping
    public Mono<String> index() {
        return webClient.get().uri("http://helloworld:5100/helloworld")
                .retrieve()
                .toEntity(String.class)
                .map(entity -> {
                    return entity.getBody();
                });
    }

    // end::ping-endpoint[]
}
