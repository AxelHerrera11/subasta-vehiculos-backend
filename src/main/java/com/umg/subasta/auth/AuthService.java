package com.umg.subasta.auth;

import com.umg.subasta.auth.AuthDtos.*;
import com.umg.subasta.common.ApiException;
import com.umg.subasta.security.AuthUser;
import com.umg.subasta.security.JwtService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository repo;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UsuarioRepository repo, PasswordEncoder encoder, JwtService jwt) {
        this.repo = repo;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public AuthResponse registrar(RegistroRequest r) {
        String correo = r.correo().trim().toLowerCase(Locale.ROOT);
        if (repo.porCorreo(correo).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        }
        try {
            int id = repo.crear(r.nombre().trim(), r.apellido().trim(), correo,
                    r.telefono().trim(), encoder.encode(r.password()));
            return respuesta(repo.porId(id).orElseThrow());
        } catch (DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese correo.");
        }
    }

    public AuthResponse login(LoginRequest r) {
        var u = repo.porCorreo(r.correo().trim().toLowerCase(Locale.ROOT))
                .filter(x -> x.activo() && encoder.matches(r.password(), x.passwordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos."));
        return respuesta(u);
    }

    public UsuarioDto yo(AuthUser user) {
        return repo.porId(user.id()).map(AuthService::dto)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Sesión no válida."));
    }

    private AuthResponse respuesta(UsuarioRepository.Usuario u) {
        String token = jwt.generar(new AuthUser(u.id(), u.correo(), u.nombre() + " " + u.apellido()));
        return new AuthResponse(token, dto(u));
    }

    private static UsuarioDto dto(UsuarioRepository.Usuario u) {
        return new UsuarioDto(u.id(), u.nombre(), u.apellido(), u.correo(), u.telefono());
    }
}
