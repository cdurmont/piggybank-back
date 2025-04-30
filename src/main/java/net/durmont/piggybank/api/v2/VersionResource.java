package net.durmont.piggybank.api.v2;

import net.durmont.piggybank.model.Version;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api-v2/version")
public class VersionResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Version Version() {
        return new Version("v2.4.0");
    }
}
