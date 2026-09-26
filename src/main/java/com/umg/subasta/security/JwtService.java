package com.umg.subasta.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class JwtService {

    private final SecretKey key;
    private final Duration vigencia;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-hours}") long horas) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe tener al menos 32 caracteres");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.vigencia = Duration.ofHours(horas);
    }

    public String generar(AuthUser u) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(u.id()))
                .claim("correo", u.correo())
                .claim("nombre", u.nombre())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(vigencia)))
                .signWith(key)
                .compact();
    }

    public Optional<AuthUser> validar(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(new AuthUser(Integer.parseInt(c.getSubject()),
                    c.get("correo", String.class), c.get("nombre", String.class)));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Convierte "Bearer xxx" en un Authentication de Spring, o vacío si no es válido. */
    public Optional<Authentication> autenticar(String header) {
        if (header == null || !header.startsWith("Bearer ")) return Optional.empty();
        return validar(header.substring(7).trim())
                .map(u -> new UsernamePasswordAuthenticationToken(u, null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
