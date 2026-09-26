package com.umg.subasta.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mensaje", ex.getMessage());
        body.putAll(ex.getExtra());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> campos = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(f -> f.getField(),
                        f -> f.getDefaultMessage() == null ? "Valor inválido" : f.getDefaultMessage(),
                        (a, b) -> a, LinkedHashMap::new));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("mensaje", "Revisa los campos marcados.");
        body.put("campos", campos);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> formato(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "El formato de la solicitud no es válido."));
    }

    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> integridad(Exception ex) {
        log.warn("Violación de integridad: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(Map.of("mensaje", "Algún dato seleccionado no es válido (revisa los catálogos y las fechas)."));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> bd(DataAccessException ex) {
        log.error("Error de base de datos", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("mensaje", "No se pudo completar la operación en la base de datos."));
    }
}
