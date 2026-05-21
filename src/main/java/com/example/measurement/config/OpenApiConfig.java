package com.example.measurement.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI measurementConversionOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Measurement Conversion API")
                        .version("1.0.0")
                        .description("Decodes encoded measurement strings into per-package totals.")
                        .contact(new Contact().name("Backend Team").email("backend@example.com"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
