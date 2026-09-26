package com.umg.subasta.subasta;

import com.umg.subasta.common.ApiException;
import com.umg.subasta.security.AuthUser;
import com.umg.subasta.subasta.SubastaDtos.*;
import com.umg.subasta.vehiculo.VehiculoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class SubastaService {

    private final VehiculoRepository repo;
    private final SimpMessagingTemplate ws;

    public SubastaService(VehiculoRepository repo, SimpMessagingTemplate ws) {
        this.repo = repo;
        this.ws = ws;
    }

    /** Toda la validación de reglas ocurre en sp_RegistrarPuja_3193 (atómica, en servidor). */
    public PujaResponse pujar(int vehiculoId, BigDecimal monto, AuthUser user) {
        BigDecimal m = monto.setScale(2, RoundingMode.HALF_UP);
        var r = repo.registrarPuja(vehiculoId, user.id(), m);

        switch (r.resultado()) {
            case "OK" -> { }
            case "NO_EXISTE" -> throw new ApiException(HttpStatus.NOT_FOUND, "El vehículo no existe.");
            case "CERRADA" -> throw new ApiException(HttpStatus.CONFLICT, "Oferta cerrada: esta subasta ya terminó.");
            case "NO_INICIADA" -> throw new ApiException(HttpStatus.CONFLICT, "La subasta aún no ha iniciado.");
            case "PROPIETARIO" -> throw new ApiException(HttpStatus.FORBIDDEN, "No puedes ofertar por tu propio vehículo.");
            case "YA_ES_LIDER" -> throw new ApiException(HttpStatus.CONFLICT, "Ya tienes la oferta más alta.");
            case "MONTO_INSUFICIENTE" -> throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "La oferta mínima en este momento es Q " + r.montoMinimo().toPlainString() + ".",
                    Map.of("montoMinimo", r.montoMinimo()));
            default -> throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo registrar la oferta.");
        }

        Instant ahora = Instant.now();
        EventoSubasta evento = new EventoSubasta("PUJA", vehiculoId, "ABIERTA", m, r.montoMinimo(),
                r.totalPujas(), ahora, ahora);
        ws.convertAndSend("/topic/subasta/" + vehiculoId, evento);
        ws.convertAndSend("/topic/subastas", evento);

        ws.convertAndSendToUser(String.valueOf(user.id()), "/queue/estado",
                new AvisoUsuario(vehiculoId, "GANANDO", m));
        if (r.anteriorLiderId() != null && r.anteriorLiderId() != user.id()) {
            ws.convertAndSendToUser(String.valueOf(r.anteriorLiderId()), "/queue/estado",
                    new AvisoUsuario(vehiculoId, "SUPERADO", m));
        }
        return new PujaResponse(m, r.montoMinimo(), r.totalPujas(), true, ahora);
    }

    public List<VehiculoRepository.PujaPublica> historial(int vehiculoId) {
        return repo.pujas(vehiculoId);
    }

    public MiEstado miEstado(int vehiculoId, AuthUser user) {
        var e = repo.estadoLider(vehiculoId, user.id())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "El vehículo no existe."));
        boolean cerrada = !"ABIERTA".equals(e.estado());
        boolean lider = e.liderId() != null && e.liderId() == user.id();
        String estado;
        if (e.usuarioId() == user.id()) estado = "PROPIETARIO";
        else if (e.miMejorPuja() == null) estado = "SIN_OFERTAS";
        else if (lider) estado = cerrada ? "GANASTE" : "GANANDO";
        else estado = cerrada ? "PERDISTE" : "SUPERADO";
        return new MiEstado(vehiculoId, estado, e.miMejorPuja());
    }

    /** Llamado por el scheduler: difunde el cierre y avisa al ganador. */
    public void notificarCierre(VehiculoRepository.Cierre c) {
        Instant ahora = Instant.now();
        EventoSubasta evento = new EventoSubasta("CIERRE", c.vehiculoId(), c.estado(), c.ofertaActual(),
                null, -1, ahora, ahora);
        ws.convertAndSend("/topic/subasta/" + c.vehiculoId(), evento);
        ws.convertAndSend("/topic/subastas", evento);
        if (c.liderId() != null) {
            ws.convertAndSendToUser(String.valueOf(c.liderId()), "/queue/estado",
                    new AvisoUsuario(c.vehiculoId(), "GANASTE", c.ofertaActual()));
        }
    }
}
