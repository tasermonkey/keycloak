package org.keycloak.services;

import javax.ws.rs.core.Response;

public class NotFoundPlainTextException extends PlainTextWebApplicationException {
    public NotFoundPlainTextException(String message) {
        super(message, Response.Status.NOT_FOUND);
    }
}
