package com.umg.subasta.vehiculo;

import com.umg.subasta.common.ApiException;
import com.umg.subasta.security.AuthUser;
import com.umg.subasta.vehiculo.VehiculoDtos.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.List;
import java.util.Map;

@Service
public class VehiculoService {

    private final VehiculoRepository repo;

    public VehiculoService(VehiculoRepository repo) {
        this.repo = repo;
    }

    public Pagina<Resumen> inventario(Filtro f) {
        List<Resumen> items = repo.buscar(f);
        return new Pagina<>(items, items.size(), Instant.now());
    }

    public Pagina<Resumen> misPublicaciones(AuthUser user, String q) {
        List<Resumen> items = repo.delUsuario(user.id(), q);
        return new Pagina<>(items, items.size(), Instant.now());
    }

    public Detalle detalle(int id, AuthUser user) {
        var fila = repo.porId(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El vehículo no existe."));
        Resumen v = fila.resumen();
        return new Detalle(v, fila.ids(), fila.descripcion(), repo.fotos(id),
                montoMinimo(v.montoBase(), v.ofertaActual()),
                user != null && user.id() == fila.usuarioId(),
                Instant.now());
    }

    /** Siguiente oferta válida: monto base si no hay pujas; si hay, oferta actual + 10%. */
    public static BigDecimal montoMinimo(BigDecimal base, BigDecimal actual) {
        if (actual == null) return base;
        return actual.multiply(new BigDecimal("1.10")).setScale(2, RoundingMode.CEILING);
    }

    @Transactional
    public Detalle crear(VehiculoRequest r, AuthUser user) {
        validar(r, true);
        int id = repo.crear(user.id(), r);
        return detalle(id, user);
    }

    @Transactional
    public Detalle actualizar(int id, VehiculoRequest r, AuthUser user) {
        var fila = repo.porId(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El vehículo no existe."));
        if (fila.usuarioId() != user.id()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo puedes editar tus propias publicaciones.");
        }
        Resumen v = fila.resumen();
        if (v.totalPujas() > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "Esta subasta ya recibió ofertas y no se puede editar.");
        }
        if (!"ABIERTA".equals(v.estado()) || !v.fechaCierre().isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.CONFLICT, "La subasta ya cerró y no se puede editar.");
        }
        validar(r, false);
        repo.actualizar(id, r);
        return detalle(id, user);
    }

    private void validar(VehiculoRequest r, boolean esNuevo) {
        int maxAnio = Year.now().getValue() + 1;
        if (r.anio() < 1950 || r.anio() > maxAnio) {
            throw campo("anio", "El año debe estar entre 1950 y " + maxAnio);
        }
        if (!repo.modeloPerteneceAMarca(r.modeloId(), r.marcaId())) {
            throw campo("modeloId", "El modelo no corresponde a la marca seleccionada");
        }
        Instant ahora = Instant.now();
        if (!r.fechaCierre().isAfter(r.fechaInicio())) {
            throw campo("fechaCierre", "El cierre debe ser posterior al inicio");
        }
        if (!r.fechaCierre().isAfter(ahora.plus(Duration.ofMinutes(5)))) {
            throw campo("fechaCierre", "El cierre debe ser al menos 5 minutos en el futuro");
        }
        if (esNuevo && r.fechaInicio().isBefore(ahora.minus(Duration.ofMinutes(10)))) {
            throw campo("fechaInicio", "La fecha de inicio no puede estar en el pasado");
        }
        long unicas = r.fotos().stream().map(FotoRequest::url).distinct().count();
        if (unicas < 5) {
            throw campo("fotos", "Sube al menos 5 fotografías distintas");
        }
    }

    private static ApiException campo(String campo, String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, msg, Map.of("campos", Map.of(campo, msg)));
    }
}
