package com.umg.subasta.common;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class SaludController {
    /** También sirve al frontend para sincronizar su reloj con el del servidor. */
    @GetMapping("/api/salud")
    public Map<String, Object> salud() {
        return Map.of("ok", true, "serverTime", Instant.now());
    }
}
