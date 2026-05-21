package com.example.measurement.service;

import com.example.measurement.dto.ConversionResponse;

/**
 * Application-layer contract for decoding measurement strings.
 *
 * <p>Defined as an interface so that controllers depend on the abstraction (DIP)
 * and so the concrete strategy can be swapped (e.g. for a streaming parser) without
 * touching the web layer.</p>
 */
public interface ConversionService {

    /**
     * Decodes the supplied measurement string and persists the request/response pair
     * to the history store.
     *
     * @param input           the raw measurement string
     * @param sourceIpAddress the originating client IP, used for audit purposes
     * @return a populated {@link ConversionResponse}
     */
    ConversionResponse convertAndRecord(String input, String sourceIpAddress);
}
