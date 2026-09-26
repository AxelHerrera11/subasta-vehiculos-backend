package com.umg.subasta.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/** Crea los 3 usuarios de prueba (con contraseña BCrypt) si todavía no existen. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private record Semilla(String nombre, String apellido, String correo, String telefono, String password) {}

    private static final List<Semilla> USUARIOS = List.of(
            new Semilla("Ana", "López", "ana@subastas.gt", "5555-0001", "Subasta#2026a"),
            new Semilla("Bruno", "Méndez", "bruno@subastas.gt", "5555-0002", "Subasta#2026b"),
            new Semilla("Carla", "Ruiz", "carla@subastas.gt", "5555-0003", "Subasta#2026c")
    );

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final boolean activo;

    public DataSeeder(UsuarioRepository repo, PasswordEncoder encoder,
                      @Value("${app.seed.users}") boolean activo) {
        this.repo = repo;
        this.encoder = encoder;
        this.activo = activo;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!activo) return;
        try {
            for (Semilla s : USUARIOS) {
                if (repo.porCorreo(s.correo()).isEmpty()) {
                    repo.crear(s.nombre(), s.apellido(), s.correo(), s.telefono(), encoder.encode(s.password()));
                    log.info("Usuario de prueba creado: {}", s.correo());
                }
            }
        } catch (Exception e) {
            log.warn("No se pudieron crear los usuarios de prueba: {}", e.getMessage());
        }
    }
}
