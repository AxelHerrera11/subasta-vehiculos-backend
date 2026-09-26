package com.umg.subasta.subasta;

import com.umg.subasta.vehiculo.VehiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Cada pocos segundos cierra subastas vencidas: VENDIDA si hubo ofertas, DESIERTA si no. */
@Component
public class CierreScheduler {

    private static final Logger log = LoggerFactory.getLogger(CierreScheduler.class);

    private final VehiculoRepository repo;
    private final SubastaService service;

    public CierreScheduler(VehiculoRepository repo, SubastaService service) {
        this.repo = repo;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${app.cierre.intervalo-ms}", initialDelay = 10000)
    public void cerrar() {
        try {
            for (var c : repo.cerrarVencidas()) {
                log.info("Subasta {} cerrada como {}", c.vehiculoId(), c.estado());
                service.notificarCierre(c);
            }
        } catch (Exception e) {
            log.warn("No se pudo ejecutar el cierre de subastas: {}", e.getMessage());
        }
    }
}
