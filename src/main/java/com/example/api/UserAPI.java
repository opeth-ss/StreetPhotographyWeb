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

    @POST
    @Path("/tags/paginated")
    public Response getPaginatedTags(PaginatedSearchRequest request) {
        List<Tag> tags = photoTagService.getTags(
                request.getQuery(),
                request.getLimit() != null ? request.getLimit() : 10,
                request.getOffset() != null ? request.getOffset() : 0
        );
        return Response.ok(tags).build();
    }

    public static class SearchRequest {
        private String query;
        private Integer limit;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
    }

    public static class PaginatedSearchRequest {
        private String query;
        private Integer limit;
        private Integer offset;

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Integer getLimit() { return limit; }
        public void setLimit(Integer limit) { this.limit = limit; }
        public Integer getOffset() { return offset; }
        public void setOffset(Integer offset) { this.offset = offset; }
    }
}