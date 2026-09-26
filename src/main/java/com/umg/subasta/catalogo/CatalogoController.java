package com.umg.subasta.catalogo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Todos los catálogos en una sola llamada (formularios y filtros). */
@RestController
@RequestMapping("/api/catalogos")
public class CatalogoController {

    public record Item(int id, String nombre) {}
    public record Modelo(int id, int marcaId, String nombre) {}
    public record Tren(int id, String codigo, String nombre) {}
    public record Danio(int id, String codigo, String nombre, String descripcion, String color) {}

    private final JdbcTemplate jdbc;

    public CatalogoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public Map<String, Object> todos() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("tiposArticulo", simple("Cat_TipoArticulo_3193"));
        r.put("marcas", simple("Cat_Marca_3193"));
        r.put("modelos", jdbc.query(
                "SELECT Id, MarcaId, Nombre FROM dbo.Cat_Modelo_3193 WHERE Activo = 1 ORDER BY Nombre",
                (rs, n) -> new Modelo(rs.getInt("Id"), rs.getInt("MarcaId"), rs.getString("Nombre"))));
        r.put("transmisiones", simple("Cat_Transmision_3193"));
        r.put("combustibles", simple("Cat_Combustible_3193"));
        r.put("trenesManejo", jdbc.query(
                "SELECT Id, Codigo, Nombre FROM dbo.Cat_TrenManejo_3193 WHERE Activo = 1 ORDER BY Codigo",
                (rs, n) -> new Tren(rs.getInt("Id"), rs.getString("Codigo"), rs.getString("Nombre"))));
        r.put("nivelesDanio", jdbc.query(
                "SELECT Id, Codigo, Nombre, Descripcion, ColorHex FROM dbo.Cat_NivelDanio_3193 ORDER BY Orden",
                (rs, n) -> new Danio(rs.getInt("Id"), rs.getString("Codigo"), rs.getString("Nombre"),
                        rs.getString("Descripcion"), rs.getString("ColorHex"))));
        return r;
    }

    // Nombre de tabla constante (no viene del usuario), sin riesgo de inyección
    private List<Item> simple(String tabla) {
        return jdbc.query("SELECT Id, Nombre FROM dbo." + tabla + " WHERE Activo = 1 ORDER BY Nombre",
                (rs, n) -> new Item(rs.getInt("Id"), rs.getString("Nombre")));
    }
}
