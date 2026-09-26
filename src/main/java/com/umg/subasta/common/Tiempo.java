package com.umg.subasta.common;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/** La BD guarda DATETIME2 en UTC; en Java trabajamos con Instant. */
public final class Tiempo {
    private Tiempo() {}

    public static LocalDateTime aBd(Instant i) {
        return i == null ? null : LocalDateTime.ofInstant(i, ZoneOffset.UTC);
    }

    public static Instant deBd(ResultSet rs, String col) throws SQLException {
        LocalDateTime ldt = rs.getObject(col, LocalDateTime.class);
        return ldt == null ? null : ldt.toInstant(ZoneOffset.UTC);
    }
}
