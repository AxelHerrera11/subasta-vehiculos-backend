package com.umg.subasta.vehiculo;

import com.umg.subasta.common.Tiempo;
import com.umg.subasta.vehiculo.VehiculoDtos.*;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class VehiculoRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public VehiculoRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    static Resumen resumen(ResultSet rs) throws SQLException {
        return new Resumen(
                rs.getInt("Id"), rs.getShort("Anio"), rs.getString("TipoArticulo"),
                rs.getString("Marca"), rs.getString("Modelo"), rs.getString("Motor"),
                rs.getString("Transmision"), rs.getString("Combustible"), rs.getString("TrenManejo"),
                rs.getShort("Cilindros"),
                new NivelDanio(rs.getString("NivelDanio"), rs.getString("NivelDanioDesc"), rs.getString("NivelDanioColor")),
                rs.getBigDecimal("MontoBase"), rs.getBigDecimal("OfertaActual"), rs.getInt("TotalPujas"),
                Tiempo.deBd(rs, "FechaInicio"), Tiempo.deBd(rs, "FechaCierre"),
                rs.getString("Estado"), rs.getString("FotoPortada"));
    }

    private static final RowMapper<Resumen> RESUMEN = (rs, n) -> resumen(rs);

    // ------------------------------------------------------------------ inventario
    public List<Resumen> buscar(Filtro f) {
        StringBuilder sql = new StringBuilder("SELECT TOP 200 * FROM dbo.vw_Inventario_3193 WHERE 1 = 1");
        MapSqlParameterSource p = new MapSqlParameterSource();

        if (f.q() != null && !f.q().isBlank()) {
            sql.append(" AND (Marca LIKE :q OR Modelo LIKE :q OR Motor LIKE :q OR TipoArticulo LIKE :q)");
            p.addValue("q", "%" + f.q().trim() + "%");
        }
        igual(sql, p, "TipoArticuloId", f.tipoArticuloId());
        igual(sql, p, "MarcaId", f.marcaId());
        igual(sql, p, "ModeloId", f.modeloId());
        igual(sql, p, "TransmisionId", f.transmisionId());
        igual(sql, p, "CombustibleId", f.combustibleId());
        igual(sql, p, "TrenManejoId", f.trenManejoId());
        igual(sql, p, "Cilindros", f.cilindros());
        if (f.nivelDanioId() != null && !f.nivelDanioId().isEmpty()) {
            sql.append(" AND NivelDanioId IN (:danio)");
            p.addValue("danio", f.nivelDanioId());
        }
        if (f.anioDesde() != null) { sql.append(" AND Anio >= :anioDesde"); p.addValue("anioDesde", f.anioDesde()); }
        if (f.anioHasta() != null) { sql.append(" AND Anio <= :anioHasta"); p.addValue("anioHasta", f.anioHasta()); }
        if (f.precioMax() != null) {
            sql.append(" AND COALESCE(OfertaActual, MontoBase) <= :precioMax");
            p.addValue("precioMax", f.precioMax());
        }

        String estado = f.estado() == null ? "VIGENTES" : f.estado().toUpperCase();
        switch (estado) {
            case "EN_CURSO" -> sql.append(" AND Estado = 'ABIERTA' AND FechaInicio <= SYSUTCDATETIME() AND FechaCierre > SYSUTCDATETIME()");
            case "PROXIMAS" -> sql.append(" AND Estado = 'ABIERTA' AND FechaInicio > SYSUTCDATETIME()");
            case "FINALIZADAS" -> sql.append(" AND (Estado <> 'ABIERTA' OR FechaCierre <= SYSUTCDATETIME())");
            case "TODAS" -> { }
            default -> sql.append(" AND Estado = 'ABIERTA' AND FechaCierre > SYSUTCDATETIME()");
        }

        String orden = f.orden() == null ? "cierre" : f.orden();
        sql.append(switch (orden) {
            case "recientes" -> " ORDER BY Id DESC";
            case "precio_asc" -> " ORDER BY COALESCE(OfertaActual, MontoBase) ASC";
            case "precio_desc" -> " ORDER BY COALESCE(OfertaActual, MontoBase) DESC";
            case "anio_desc" -> " ORDER BY Anio DESC, Id DESC";
            default -> " ORDER BY CASE WHEN FechaCierre > SYSUTCDATETIME() THEN 0 ELSE 1 END, FechaCierre ASC";
        });
        return jdbc.query(sql.toString(), p, RESUMEN);
    }

    private static void igual(StringBuilder sql, MapSqlParameterSource p, String col, Integer val) {
        if (val != null) {
            String param = "p" + col;
            sql.append(" AND ").append(col).append(" = :").append(param);
            p.addValue(param, val);
        }
    }

    public List<Resumen> delUsuario(int usuarioId, String q) {
        MapSqlParameterSource p = new MapSqlParameterSource("u", usuarioId);
        String sql = "SELECT * FROM dbo.vw_Inventario_3193 WHERE UsuarioId = :u";
        if (q != null && !q.isBlank()) {
            sql += " AND (Marca LIKE :q OR Modelo LIKE :q OR Motor LIKE :q OR CAST(Anio AS VARCHAR(4)) LIKE :q)";
            p.addValue("q", "%" + q.trim() + "%");
        }
        return jdbc.query(sql + " ORDER BY Id DESC", p, RESUMEN);
    }

    // ------------------------------------------------------------------ detalle
    public record Fila(Resumen resumen, Ids ids, String descripcion, int usuarioId) {}

    public Optional<Fila> porId(int id) {
        return jdbc.query("SELECT * FROM dbo.vw_Inventario_3193 WHERE Id = :id",
                new MapSqlParameterSource("id", id),
                (rs, n) -> new Fila(resumen(rs),
                        new Ids(rs.getInt("TipoArticuloId"), rs.getInt("MarcaId"), rs.getInt("ModeloId"),
                                rs.getInt("TransmisionId"), rs.getInt("CombustibleId"),
                                rs.getInt("TrenManejoId"), rs.getInt("NivelDanioId")),
                        rs.getString("Descripcion"), rs.getInt("UsuarioId")))
                .stream().findFirst();
    }

    public List<Foto> fotos(int vehiculoId) {
        return jdbc.query("SELECT Id, Url, PublicId, Orden FROM dbo.FotosVehiculo_3193 WHERE VehiculoId = :v ORDER BY Orden",
                new MapSqlParameterSource("v", vehiculoId),
                (rs, n) -> new Foto(rs.getInt("Id"), rs.getString("Url"), rs.getString("PublicId"), rs.getInt("Orden")));
    }

    public boolean modeloPerteneceAMarca(int modeloId, int marcaId) {
        Integer c = jdbc.queryForObject("SELECT COUNT(*) FROM dbo.Cat_Modelo_3193 WHERE Id = :m AND MarcaId = :ma",
                new MapSqlParameterSource("m", modeloId).addValue("ma", marcaId), Integer.class);
        return c != null && c > 0;
    }

    // ------------------------------------------------------------------ escritura
    private MapSqlParameterSource params(VehiculoRequest r) {
        return new MapSqlParameterSource()
                .addValue("anio", r.anio())
                .addValue("tipo", r.tipoArticuloId())
                .addValue("marca", r.marcaId())
                .addValue("modelo", r.modeloId())
                .addValue("motor", r.motor().trim())
                .addValue("transm", r.transmisionId())
                .addValue("comb", r.combustibleId())
                .addValue("tren", r.trenManejoId())
                .addValue("cil", r.cilindros())
                .addValue("danio", r.nivelDanioId())
                .addValue("desc", r.descripcion() == null || r.descripcion().isBlank() ? null : r.descripcion().trim())
                .addValue("base", r.montoBase())
                .addValue("ini", Tiempo.aBd(r.fechaInicio()))
                .addValue("fin", Tiempo.aBd(r.fechaCierre()));
    }

    public int crear(int usuarioId, VehiculoRequest r) {
        Integer id = jdbc.queryForObject("""
                INSERT INTO dbo.Vehiculos_3193
                    (UsuarioId, Anio, TipoArticuloId, MarcaId, ModeloId, Motor, TransmisionId, CombustibleId,
                     TrenManejoId, Cilindros, NivelDanioId, Descripcion, MontoBase, FechaInicio, FechaCierre)
                OUTPUT INSERTED.Id
                VALUES (:u, :anio, :tipo, :marca, :modelo, :motor, :transm, :comb,
                        :tren, :cil, :danio, :desc, :base, :ini, :fin)""",
                params(r).addValue("u", usuarioId), Integer.class);
        insertarFotos(id, r.fotos());
        return id;
    }

    public void actualizar(int id, VehiculoRequest r) {
        jdbc.update("""
                UPDATE dbo.Vehiculos_3193 SET
                    Anio = :anio, TipoArticuloId = :tipo, MarcaId = :marca, ModeloId = :modelo, Motor = :motor,
                    TransmisionId = :transm, CombustibleId = :comb, TrenManejoId = :tren, Cilindros = :cil,
                    NivelDanioId = :danio, Descripcion = :desc, MontoBase = :base,
                    FechaInicio = :ini, FechaCierre = :fin, FechaModificacion = SYSUTCDATETIME()
                WHERE Id = :id""", params(r).addValue("id", id));
        jdbc.update("DELETE FROM dbo.FotosVehiculo_3193 WHERE VehiculoId = :v", new MapSqlParameterSource("v", id));
        insertarFotos(id, r.fotos());
    }

    private void insertarFotos(int vehiculoId, List<FotoRequest> fotos) {
        List<MapSqlParameterSource> batch = new ArrayList<>();
        for (int i = 0; i < fotos.size(); i++) {
            batch.add(new MapSqlParameterSource()
                    .addValue("v", vehiculoId)
                    .addValue("url", fotos.get(i).url())
                    .addValue("pid", fotos.get(i).publicId())
                    .addValue("o", i + 1));
        }
        jdbc.batchUpdate("INSERT INTO dbo.FotosVehiculo_3193 (VehiculoId, Url, PublicId, Orden) VALUES (:v, :url, :pid, :o)",
                batch.toArray(MapSqlParameterSource[]::new));
    }

    // ------------------------------------------------------------------ pujas
    public record PujaPublica(BigDecimal monto, java.time.Instant fecha) {}

    /** Historial SIN identidad del postor. */
    public List<PujaPublica> pujas(int vehiculoId) {
        return jdbc.query("SELECT TOP 15 Monto, FechaPuja FROM dbo.Pujas_3193 WHERE VehiculoId = :v ORDER BY Id DESC",
                new MapSqlParameterSource("v", vehiculoId),
                (rs, n) -> new PujaPublica(rs.getBigDecimal("Monto"), Tiempo.deBd(rs, "FechaPuja")));
    }

    public record EstadoLider(Integer liderId, int usuarioId, String estado, BigDecimal miMejorPuja) {}

    public Optional<EstadoLider> estadoLider(int vehiculoId, int usuarioId) {
        return jdbc.query("""
                SELECT v.LiderUsuarioId, v.UsuarioId, v.Estado,
                       (SELECT MAX(p.Monto) FROM dbo.Pujas_3193 p WHERE p.VehiculoId = v.Id AND p.UsuarioId = :u) AS MiMejor
                FROM dbo.Vehiculos_3193 v WHERE v.Id = :v""",
                new MapSqlParameterSource("v", vehiculoId).addValue("u", usuarioId),
                (rs, n) -> new EstadoLider(rs.getObject("LiderUsuarioId", Integer.class), rs.getInt("UsuarioId"),
                        rs.getString("Estado"), rs.getBigDecimal("MiMejor")))
                .stream().findFirst();
    }

    public record ResultadoPuja(String resultado, BigDecimal montoMinimo, Integer anteriorLiderId, int totalPujas) {}

    public ResultadoPuja registrarPuja(int vehiculoId, int usuarioId, BigDecimal monto) {
        return jdbc.queryForObject("EXEC dbo.sp_RegistrarPuja_3193 @VehiculoId = :v, @UsuarioId = :u, @Monto = :m",
                new MapSqlParameterSource("v", vehiculoId).addValue("u", usuarioId).addValue("m", monto),
                (rs, n) -> {
                    String res = rs.getString("Resultado");
                    return new ResultadoPuja(res, rs.getBigDecimal("MontoMinimo"),
                            rs.getObject("AnteriorLiderId", Integer.class),
                            "OK".equals(res) ? rs.getInt("TotalPujas") : 0);
                });
    }

    public record Cierre(int vehiculoId, String estado, BigDecimal ofertaActual, Integer liderId) {}

    public List<Cierre> cerrarVencidas() {
        return jdbc.query("EXEC dbo.sp_CerrarSubastasVencidas_3193", new MapSqlParameterSource(),
                (rs, n) -> new Cierre(rs.getInt("Id"), rs.getString("Estado"),
                        rs.getBigDecimal("OfertaActual"), rs.getObject("LiderUsuarioId", Integer.class)));
    }
}
