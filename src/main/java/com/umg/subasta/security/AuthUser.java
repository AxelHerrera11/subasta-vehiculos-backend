package com.umg.subasta.security;

import java.security.Principal;

/** Usuario autenticado. getName() = id, que se usa como destino de /user/queue en WebSocket. */
public record AuthUser(int id, String correo, String nombre) implements Principal {
    @Override
    public String getName() {
        return String.valueOf(id);
    }
}
