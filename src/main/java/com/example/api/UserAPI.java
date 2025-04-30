package com.example.api;

import com.example.model.Tag;
import com.example.services.PhotoTagService;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserAPI {

    @Inject
    private PhotoTagService photoTagService;

    @POST
    @Path("/tags")
    public Response getTags(SearchRequest request) {
        List<Tag> tags = photoTagService.getTags(request.getQuery(), request.getLimit());
        return Response.ok(tags).build();
    }

    // Add this class somewhere in your API package
    public static class SearchRequest {
        private String query;
        private Integer limit;

        // getters and setters
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }
}
