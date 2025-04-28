package com.example.api;

import com.example.model.User;
import com.example.services.AuthenticationService;
import com.example.utils.JWTRequired;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminAPI {
    @Inject
    private AuthenticationService authenticationService;

    @POST
    @Path("/userlist")
    @JWTRequired
    public Response userlist(Map<String, Object> requestParams) {
        try {
            // Extract pagination and sorting parameters
            int page = Optional.ofNullable(requestParams.get("page")).map(p -> Integer.parseInt(p.toString())).orElse(1);
            int size = Optional.ofNullable(requestParams.get("size")).map(s -> Integer.parseInt(s.toString())).orElse(10);
            String sortField = Optional.ofNullable(requestParams.get("sortField")).map(Object::toString).orElse(null);
            String sortOrder = Optional.ofNullable(requestParams.get("sortOrder")).map(Object::toString).orElse(null);

            // Extract filters
            Map<String, Object> filters = Optional.ofNullable((Map<String, Object>) requestParams.get("filters"))
                    .orElse(new HashMap<>());

            // Validate pagination parameters
            if (page < 1 || size < 1) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Page and size must be positive integers")
                        .build();
            }

            // Retrieve paginated users and total count
            Map<String, Object> response = new HashMap<>();
            List<User> userList = authenticationService.findAllPaginated(page, size, sortField, sortOrder, filters);
            userList.forEach(user -> user.setJoinDate(null));
            userList.forEach(user -> user.setPassword(null));
            response.put("users", userList);
            response.put("totalRecords", authenticationService.getTotalUserCount(filters));

            return Response.ok()
                    .entity(response)
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred while retrieving the user list: " + e.getMessage())
                    .build();
        }
    }

    @PUT
    @Path("/updateUser")
    @JWTRequired
    public Response updateUser(User updateUser, @Context HttpServletRequest request) {
        try {
            // Input validation with Optional
            return Optional.ofNullable(updateUser)
                    .filter(u -> u.getId() != null)
                    .map(u -> {
                        User existingUser = authenticationService.findById(u.getId());
                        if (existingUser == null) {
                            return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
                        }

                        // Update fields using Optional to avoid null checks
                        Optional.ofNullable(u.getUserName()).ifPresent(existingUser::setUserName);
                        Optional.ofNullable(u.getEmail()).ifPresent(existingUser::setEmail);
                        Optional.ofNullable(u.getRole()).ifPresent(existingUser::setRole);

                        // Preserve existing password
                        existingUser.setPassword(existingUser.getPassword());

                        // Save and hide password in response
                        User updatedUser = authenticationService.updateNew(existingUser);
                        updatedUser.setPassword(null);

                        return Response.ok(updatedUser).entity("User updated successfully").build();
                    })
                    .orElse(Response.status(Response.Status.BAD_REQUEST).entity("User ID is required").build());
        } catch (Exception e) {
            return Response.serverError().entity("Update failed: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/deleteUser/{userId}")
    @JWTRequired
    public Response deleteUser(@PathParam("userId") Long userId, @Context HttpServletRequest request) {
        try {
            // Validate input with Optional
            return Optional.ofNullable(userId)
                    .map(id -> {
                        User existingUser = authenticationService.findById(id);
                        if (existingUser == null) {
                            return Response.status(Response.Status.NOT_FOUND)
                                    .entity("User not found with ID: " + id)
                                    .build();
                        }

                        authenticationService.deleteUser(existingUser);
                        return Response.ok().entity("User deleted successfully").build();
                    })
                    .orElse(Response.status(Response.Status.BAD_REQUEST).entity("User ID is required").build());
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred while deleting the user: " + e.getMessage())
                    .build();
        }
    }
}