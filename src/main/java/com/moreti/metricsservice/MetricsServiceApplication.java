package com.moreti.metricsservice;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class MetricsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetricsServiceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) { return builder.build(); }

    @RestController
    class HelloController {

        private static final Logger LOGGER = LoggerFactory.getLogger(HelloController.class);
        private final RestTemplate restTemplate;
        private final SleepService sleepService;
//        private final Tracer tracer;
        private final ObservationRegistry observationRegistry;

        HelloController(RestTemplate restTemplate, SleepService sleepService, ObservationRegistry observationRegistry) {
            this.restTemplate = restTemplate;
            this.sleepService = sleepService;
            this.observationRegistry = observationRegistry;
        }

        @GetMapping("/hello")
        public String hello() {
            LOGGER.info("---------Hello method started---------");
            LOGGER.error("---------Hello method started, id missing!---------");
            ResponseEntity<String> response = restTemplate.postForEntity("https://httpbin.org/post", "Hello, Cloud!", String.class);
            return response.getBody();
        }

        @GetMapping("/exception")
        public String exception() {
            throw new IllegalArgumentException("This id is invalid");
        }

        @GetMapping("/sleep")
        public Long sleep(@RequestParam Long ms) {
            // Span
//            Span newSpan = this.tracer.nextSpan().name("do-sleep-method-span");
//            try(Tracer.SpanInScope spanInScope = this.tracer.withSpan(newSpan.start())) {
//                return this.sleepService.doSleep(ms);
//            } finally {
//                newSpan.end();
//            }
            // Observation
//            Long result = Observation.createNotStarted("do.sleep.method.timed", this.observationRegistry) // metric name
//                    .contextualName("do-sleep-method-span") // span name
//                    .lowCardinalityKeyValue("low", "low") // tags for metric and span
//                    .highCardinalityKeyValue("high", "high") // tags for traces (span)
//                    .observe(() -> this.sleepService.doSleep(ms));
//            return result;
            return this.sleepService.doSleep(ms);
        }

        @ExceptionHandler(value = { IllegalArgumentException.class })
        protected ResponseEntity<String> handleConflict(IllegalArgumentException ex) {
            LOGGER.error(ex.getMessage(), ex);
            return ResponseEntity.badRequest().body(ex.getMessage());
        }

    }

    @Service
    class SleepService {
//      @Timed(value = "do.sleep.method.timed")
//		@NewSpan(value = "do-sleep-method-span")
        @Observed(name = "do.sleep.method.timed", contextualName = "do-sleep-method-span", lowCardinalityKeyValues = {"low", "low"})
        public Long doSleep(Long ms) {
            try {
                TimeUnit.MILLISECONDS.sleep(ms);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return ms;
        }
    }

}
