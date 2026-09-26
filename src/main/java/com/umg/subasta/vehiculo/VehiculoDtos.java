package com.umg.subasta.vehiculo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class VehiculoDtos {
    private VehiculoDtos() {}

    public record FotoRequest(
            @NotBlank(message = "La URL de la foto es obligatoria")
            @Pattern(regexp = "^https://.+", message = "La foto debe ser una URL https") @Size(max = 500) String url,
            @Size(max = 200) String publicId) {}

    public record VehiculoRequest(
            @NotNull(message = "El año es obligatorio") Short anio,
            @NotNull(message = "El tipo de artículo es obligatorio") Integer tipoArticuloId,
            @NotNull(message = "La marca es obligatoria") Integer marcaId,
            @NotNull(message = "El modelo es obligatorio") Integer modeloId,
            @NotBlank(message = "El motor es obligatorio") @Size(max = 60) String motor,
            @NotNull(message = "La transmisión es obligatoria") Integer transmisionId,
            @NotNull(message = "El combustible es obligatorio") Integer combustibleId,
            @NotNull(message = "El tren de manejo es obligatorio") Integer trenManejoId,
            @NotNull(message = "El número de cilindros es obligatorio")
            @Min(value = 0, message = "Cilindros entre 0 y 16") @Max(value = 16, message = "Cilindros entre 0 y 16") Short cilindros,
            @NotNull(message = "El nivel de daño es obligatorio") Integer nivelDanioId,
            @Size(max = 1000) String descripcion,
            @NotNull(message = "El monto base es obligatorio")
            @DecimalMin(value = "1.00", message = "El monto base debe ser mayor a 0")
            @Digits(integer = 10, fraction = 2, message = "Monto no válido") BigDecimal montoBase,
            @NotNull(message = "La fecha de inicio es obligatoria") Instant fechaInicio,
            @NotNull(message = "La fecha de cierre es obligatoria") Instant fechaCierre,
            @NotNull(message = "Las fotos son obligatorias")
            @Size(min = 5, max = 12, message = "Sube entre 5 y 12 fotografías") List<@Valid FotoRequest> fotos) {}

    public record NivelDanio(String codigo, String descripcion, String color) {}

    public record Resumen(
            int id, short anio, String tipoArticulo, String marca, String modelo, String motor,
            String transmision, String combustible, String trenManejo, short cilindros,
            NivelDanio nivelDanio, BigDecimal montoBase, BigDecimal ofertaActual, int totalPujas,
            Instant fechaInicio, Instant fechaCierre, String estado, String fotoPortada) {}

    public record Foto(int id, String url, String publicId, int orden) {}

    public record Detalle(
            Resumen vehiculo,
            Ids ids,
            String descripcion,
            List<Foto> fotos,
            BigDecimal montoMinimo,
            boolean esPropio,
            Instant serverTime) {}

    /** Ids de catálogo, para precargar el formulario de edición. */
    public record Ids(int tipoArticuloId, int marcaId, int modeloId, int transmisionId,
                      int combustibleId, int trenManejoId, int nivelDanioId) {}

    public record Filtro(
            String q,
            Integer tipoArticuloId, Integer marcaId, Integer modeloId,
            Integer transmisionId, Integer combustibleId, Integer trenManejoId,
            Integer cilindros, List<Integer> nivelDanioId,
            Integer anioDesde, Integer anioHasta,
            BigDecimal precioMax,
            String estado,   // VIGENTES (defecto) | EN_CURSO | PROXIMAS | FINALIZADAS | TODAS
            String orden) {} // cierre (defecto) | recientes | precio_asc | precio_desc | anio_desc

    public record Pagina<T>(List<T> items, int total, Instant serverTime) {}
}
