package com.umg.subasta.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Salud del servicio. También sirve al frontend para sincronizar su reloj con el del servidor.
 *
 * <p>Verifica la base de datos a propósito: si solo comprobara que el proceso vive,
 * un despliegue con la BD inaccesible se vería "sano" y el fallo pasaría desapercibido.
 * Con la BD caída responde 503 y deja el motivo en el log.
 */
@RestController
public class SaludController {

    private static final Logger log = LoggerFactory.getLogger(SaludController.class);

    private final JdbcTemplate jdbc;

    public SaludController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/salud")
    public ResponseEntity<Map<String, Object>> salud() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("serverTime", Instant.now());
        try {
            jdbc.queryForObject("SELECT 1", Integer.class);
            r.put("ok", true);
            r.put("bd", "ok");
            return ResponseEntity.ok(r);
        } catch (DataAccessException e) {
            log.error("La base de datos no responde en /api/salud", e);
            r.put("ok", false);
            r.put("bd", "error");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(r);
        }
    }
}
