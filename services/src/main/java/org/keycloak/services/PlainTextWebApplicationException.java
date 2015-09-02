package org.keycloak.services;


import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class PlainTextWebApplicationException extends WebApplicationException {
    public PlainTextWebApplicationException(String message, Response.Status status) {
        super(Response.status(status).type(MediaType.TEXT_PLAIN).entity(message).build());
    }
}
