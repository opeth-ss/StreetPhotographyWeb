package com.example.cors;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Provider
public class CORS implements ContainerResponseFilter {

    private static final List<String> allowedOrigins = Arrays.asList(
            "http://localhost:5173",
            "http://localhost:4173",
            "http://localhost:3000",
            "http://localhost:80",
            "http://localhost"
    );

    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        String origin = requestContext.getHeaderString("Origin");

        // If the origin is in our allowed list, set it as the allowed origin
        if (origin != null && allowedOrigins.contains(origin)) {
            responseContext.getHeaders().add(
                    "Access-Control-Allow-Origin", "*");
            responseContext.getHeaders().add(
                    "Access-Control-Allow-Credentials", "true");
        }

        responseContext.getHeaders().add(
                "Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization");
        responseContext.getHeaders().add(
                "Access-Control-Allow-Methods",
                "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}