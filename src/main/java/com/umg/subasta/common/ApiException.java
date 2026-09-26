package com.umg.subasta.common;

import org.springframework.http.HttpStatus;

import java.util.Map;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final Map<String, Object> extra;

    public ApiException(HttpStatus status, String mensaje) {
        this(status, mensaje, Map.of());
    }

    public ApiException(HttpStatus status, String mensaje, Map<String, Object> extra) {
        super(mensaje);
        this.status = status;
        this.extra = extra;
    }

    public HttpStatus getStatus() { return status; }
    public Map<String, Object> getExtra() { return extra; }
}
