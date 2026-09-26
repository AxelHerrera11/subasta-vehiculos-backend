package com.umg.subasta.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Optional;

@Repository
public class UsuarioRepository {

    public record Usuario(int id, String nombre, String apellido, String correo,
                          String telefono, String passwordHash, boolean activo) {}

    private static final RowMapper<Usuario> MAPPER = (rs, n) -> new Usuario(
            rs.getInt("Id"), rs.getString("Nombre"), rs.getString("Apellido"),
            rs.getString("Correo"), rs.getString("Telefono"),
            rs.getString("PasswordHash"), rs.getBoolean("Activo"));

    private final JdbcTemplate jdbc;

    public UsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Usuario> porCorreo(String correo) {
        return jdbc.query("SELECT * FROM dbo.Usuarios_3193 WHERE Correo = ?", MAPPER, correo)
                .stream().findFirst();
    }

    public Optional<Usuario> porId(int id) {
        return jdbc.query("SELECT * FROM dbo.Usuarios_3193 WHERE Id = ?", MAPPER, id)
                .stream().findFirst();
    }

    public int crear(String nombre, String apellido, String correo, String telefono, String hash) {
        GeneratedKeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement("""
                    INSERT INTO dbo.Usuarios_3193 (Nombre, Apellido, Correo, Telefono, PasswordHash)
                    VALUES (?, ?, ?, ?, ?)""", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, nombre);
            ps.setString(2, apellido);
            ps.setString(3, correo);
            ps.setString(4, telefono);
            ps.setString(5, hash);
            return ps;
        }, kh);
        return kh.getKey().intValue();
    }
}
