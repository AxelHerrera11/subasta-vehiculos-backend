package com.umg.subasta.subasta;

import com.umg.subasta.security.AuthUser;
import com.umg.subasta.subasta.SubastaDtos.*;
import com.umg.subasta.vehiculo.VehiculoRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos/{id}")
public class SubastaController {

    private final SubastaService service;

    public SubastaController(SubastaService service) {
        this.service = service;
    }

    /** Requiere login. */
    @PostMapping("/pujas")
    public PujaResponse pujar(@PathVariable int id, @Valid @RequestBody PujaRequest req,
                              @AuthenticationPrincipal AuthUser user) {
        return service.pujar(id, req.monto(), user);
    }

    /** Público: solo montos y fechas, nunca identidades. */
    @GetMapping("/pujas")
    public List<VehiculoRepository.PujaPublica> historial(@PathVariable int id) {
        return service.historial(id);
    }

    /** Requiere login: ¿voy ganando esta subasta? */
    @GetMapping("/mi-estado")
    public MiEstado miEstado(@PathVariable int id, @AuthenticationPrincipal AuthUser user) {
        return service.miEstado(id, user);
    }
}
