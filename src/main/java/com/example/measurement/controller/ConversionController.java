package com.example.measurement.controller;

import com.example.measurement.dto.ConversionResponse;
import com.example.measurement.service.ConversionService;
import com.example.measurement.util.IpAddressUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conversion")
@Validated
@Tag(name = "Conversion", description = "Decodes measurement strings into per-package totals")
public class ConversionController {

    private final ConversionService conversionService;

    public ConversionController(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    @GetMapping
    @Operation(summary = "Convert a measurement string into a list of per-package totals")
    public ResponseEntity<ConversionResponse> convert(
            @RequestParam("input")
            @NotBlank(message = "input must not be blank")
            @Size(max = 10_000, message = "input must be at most 10000 characters")
            @Pattern(regexp = "^[A-Za-z_]+$",
                     message = "input may only contain letters (A-Z, a-z) and underscores")
            String input,
            HttpServletRequest request) {

        String ip = IpAddressUtil.resolve(request);
        ConversionResponse response = conversionService.convertAndRecord(input, ip);
        return ResponseEntity.ok(response);
    }
}
