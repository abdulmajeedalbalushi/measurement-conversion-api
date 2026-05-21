package com.example.measurement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class
MeasurementConversionApplication {

    private static final Logger log = LoggerFactory.getLogger(MeasurementConversionApplication.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(MeasurementConversionApplication.class, args);
        Environment env = ctx.getEnvironment();
        log.info("=========================================================");
        log.info(" {} started successfully", env.getProperty("spring.application.name"));
        log.info(" Profile(s)   : {}", String.join(",", env.getActiveProfiles()));
        log.info(" Server port  : {}", env.getProperty("server.port"));
        log.info(" Swagger UI   : http://localhost:{}/swagger-ui.html", env.getProperty("server.port"));
        log.info(" Health probe : http://localhost:{}/actuator/health", env.getProperty("server.port"));
        log.info("=========================================================");
    }
}
