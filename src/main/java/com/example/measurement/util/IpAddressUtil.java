package com.example.measurement.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Resolves the originating client IP address, honouring common reverse-proxy headers.
 */
public final class IpAddressUtil {

    private static final String[] PROXY_HEADERS = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
    };

    private IpAddressUtil() { }

    public static String resolve(HttpServletRequest request) {
        if (request == null) return "unknown";
        for (String header : PROXY_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value)) {
                // X-Forwarded-For can be a comma-separated chain; take the first hop.
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        return request.getRemoteAddr();
    }
}
