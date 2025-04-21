package com.example.api;

import com.example.model.User;
import com.example.services.AuthenticationService;
import com.example.utils.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthAPI {
    @Inject
    private AuthenticationService authenticationService;

    @POST
    @Path("/login")
    public Response login(User loginRequest, @Context HttpServletRequest request){
        User user = authenticationService.findByUsername(loginRequest.getUserName());
        if (user != null && BCrypt.checkpw(loginRequest.getPassword(), user.getPassword())) {
            String token = JwtUtil.generateToken(user.getUserName(), user.getRole());
            user.setPassword(null);
            user.setJoinDate(null);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("token", token);
            responseMap.put("user", user);

            return Response.ok(responseMap).build();
        } else {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid username or password.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

    @POST
    @Path("/register")
    public Response register(User registerRequest, @Context HttpServletRequest request) {
        if (registerRequest.getUserName() == null || registerRequest.getPassword() == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\":\"Username and password are required.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        boolean registered = authenticationService.registerUser(registerRequest);
        if (registered) {
            Map<String, Boolean> responseMap = new HashMap<>();
            responseMap.put("success", true);
            return Response.ok(responseMap).build();
        } else {
            return Response.status(Response.Status.CONFLICT)
                    .entity("{\"error\":\"Username already exists.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
