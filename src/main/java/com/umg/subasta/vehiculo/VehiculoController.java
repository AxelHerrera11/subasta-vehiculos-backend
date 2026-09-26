package com.umg.subasta.vehiculo;

import com.umg.subasta.security.AuthUser;
import com.umg.subasta.vehiculo.VehiculoDtos.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class VehiculoController {

    private final VehiculoService service;

    public VehiculoController(VehiculoService service) {
        this.service = service;
    }

    /** Inventario público con filtros (anónimos pueden verlo). */
    @GetMapping("/vehiculos")
    public Pagina<Resumen> inventario(@ModelAttribute Filtro filtro) {
        return service.inventario(filtro);
    }

    /** Detalle público; si hay sesión, indica si el vehículo es propio. */
    @GetMapping("/vehiculos/{id}")
    public Detalle detalle(@PathVariable int id, @AuthenticationPrincipal AuthUser user) {
        return service.detalle(id, user);
    }

    @PostMapping("/vehiculos")
    @ResponseStatus(HttpStatus.CREATED)
    public Detalle publicar(@Valid @RequestBody VehiculoRequest req, @AuthenticationPrincipal AuthUser user) {
        return service.crear(req, user);
    }

    @PutMapping("/vehiculos/{id}")
    public Detalle editar(@PathVariable int id, @Valid @RequestBody VehiculoRequest req,
                          @AuthenticationPrincipal AuthUser user) {
        return service.actualizar(id, req, user);
    }

    @GetMapping("/mis-publicaciones")
    public Pagina<Resumen> misPublicaciones(@RequestParam(required = false) String q,
                                            @AuthenticationPrincipal AuthUser user) {
        return service.misPublicaciones(user, q);
    }
}
