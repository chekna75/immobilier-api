package com.ditsolution.common.services;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

public abstract class BaseService {

    // ---------- String helpers ----------
    protected boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    protected String trim(String s) {
        return s == null ? null : s.trim();
    }

    protected <T> List<T> safeList(List<T> l) {
        return l == null ? List.of() : l;
    }

    // ---------- HTTP error helpers ----------
    protected WebApplicationException badRequest(String code, String msg) {
        return error(Response.Status.BAD_REQUEST, code, msg);
    }

    protected WebApplicationException forbidden(String code, String msg) {
        return error(Response.Status.FORBIDDEN, code, msg);
    }

    protected WebApplicationException notFound(String code, String msg) {
        return error(Response.Status.NOT_FOUND, code, msg);
    }

    protected WebApplicationException unauthorized(String code, String msg) {
        return error(Response.Status.UNAUTHORIZED, code, msg);
    }

    protected WebApplicationException conflict(String code, String msg) {
        return error(Response.Status.CONFLICT, code, msg);
    }

    private WebApplicationException error(Response.Status status, String code, String msg) {
        var body = Map.of("error", code, "message", msg);
        return new WebApplicationException(Response.status(status).entity(body).build());
    }
}
