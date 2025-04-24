package com.example.api;

import com.example.model.User;
import com.example.services.AuthenticationService;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AdminAPI {
    @Inject
    private AuthenticationService authenticationService;

    @POST
    @Path("/userlist")
    public Response userlist(@Context HttpServletRequest request) {
        try {
            // Retrieve the list of users
            List<User> userList = authenticationService.findAll();

            userList.forEach(user -> user.setPassword(null));
            userList.forEach(user -> user.setJoinDate(null));

            // Return the user list in the response
            return Response.ok(userList)
                    .build();
        } catch (Exception e) {
            // Handle any unexpected errors
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred while retrieving the user list")
                    .build();
        }
    }

    @PUT
    @Path("/updateUser")
    public Response updateUser(User updateUser, @Context HttpServletRequest request) {
        try {
            // Input validation
            if (updateUser == null || updateUser.getId() == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("User ID is required").build();
            }

            // Fetch existing user
            User existingUser = authenticationService.findById(updateUser.getId());
            if (existingUser == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
            }

            // Update only allowed fields (explicitly skip password)
            if (updateUser.getUserName() != null) {
                existingUser.setUserName(updateUser.getUserName());
            }
            if (updateUser.getEmail() != null) {
                existingUser.setEmail(updateUser.getEmail());
            }
            if (updateUser.getRole() != null) {
                existingUser.setRole(updateUser.getRole());
            }

            // Ensure password is not overwritten
            existingUser.setPassword(existingUser.getPassword()); // Keep original password

            // Save
            User updatedUser = authenticationService.updateNew(existingUser);
            updatedUser.setPassword(null); // Hide password in response

            return Response.ok(updatedUser).entity("User updated successfully").build();
        } catch (Exception e) {
            return Response.serverError().entity("Update failed: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/deleteUser/{userId}")
    public Response deleteUser(@PathParam("userId") Long userId, @Context HttpServletRequest request) {
        try {
            // Validate input
            if (userId == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("User ID is required")
                        .build();
            }

            // Check if user exists
            User existingUser = authenticationService.findById(userId);
            if (existingUser == null) {
                return Response.status(Response.Status.NOT_FOUND)
                        .entity("User not found with ID: " + userId)
                        .build();
            }

            // Delete the user
            authenticationService.deleteUser(authenticationService.findById(userId));

            return Response.ok()
                    .entity("User deleted successfully")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("An error occurred while deleting the user: " + e.getMessage())
                    .build();
        }
    }
}