package com.example.measurement;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MeasurementConversionApplicationTests {

    @Test
    void contextLoads() {
        // smoke test: application context boots under the "test" profile (H2)
    }
}
