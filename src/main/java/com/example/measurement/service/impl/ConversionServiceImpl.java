package com.example.measurement.service.impl;

import com.example.measurement.dto.ConversionResponse;
import com.example.measurement.dto.HistoryDto;
import com.example.measurement.service.ConversionService;
import com.example.measurement.service.HistoryService;
import com.example.measurement.util.MeasurementParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Default implementation of {@link ConversionService}. Coordinates the parser
 * (domain logic) with the history service (persistence side-effect).
 *
 * <p>Demonstrates Single Responsibility: this class neither parses nor stores
 * — it composes the two collaborators behind one cohesive use case.</p>
 */
@Service
public class ConversionServiceImpl implements ConversionService {

    private static final Logger log = LoggerFactory.getLogger(ConversionServiceImpl.class);

    private final MeasurementParser parser;
    private final HistoryService historyService;

    public ConversionServiceImpl(MeasurementParser parser, HistoryService historyService) {
        this.parser = parser;
        this.historyService = historyService;
    }

    @Override
    public ConversionResponse convertAndRecord(String input, String sourceIpAddress) {
        log.debug("Decoding measurement string of length {} from {}",
                input == null ? 0 : input.length(), sourceIpAddress);

        List<Integer> packages = parser.parse(input);
        ConversionResponse response = new ConversionResponse(input, packages);

        HistoryDto history = new HistoryDto(
                null, Instant.now(), sourceIpAddress, input, packages.toString());
        historyService.create(history);

        log.info("Decoded {} package(s) for ip={}", packages.size(), sourceIpAddress);
        return response;
    }
}
