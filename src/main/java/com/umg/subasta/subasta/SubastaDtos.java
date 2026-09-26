package com.umg.subasta.subasta;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public final class SubastaDtos {
    private SubastaDtos() {}

    public record PujaRequest(
            @NotNull(message = "Indica el monto de tu oferta")
            @DecimalMin(value = "1.00", message = "Monto no válido")
            @Digits(integer = 10, fraction = 2, message = "Monto no válido") BigDecimal monto) {}

    /** Evento público: nunca incluye quién ofertó. tipo = PUJA | CIERRE */
    public record EventoSubasta(String tipo, int vehiculoId, String estado,
                                BigDecimal ofertaActual, BigDecimal montoMinimo,
                                int totalPujas, Instant fecha, Instant serverTime) {}

    /** Aviso privado a un usuario. estado = GANANDO | SUPERADO | GANASTE */
    public record AvisoUsuario(int vehiculoId, String estado, BigDecimal ofertaActual) {}

    public record PujaResponse(BigDecimal ofertaActual, BigDecimal montoMinimo, int totalPujas,
                               boolean ganando, Instant serverTime) {}

    /** estadoPostor = SIN_OFERTAS | GANANDO | SUPERADO | GANASTE | PERDISTE | PROPIETARIO */
    public record MiEstado(int vehiculoId, String estadoPostor, BigDecimal miMejorPuja) {}
}
