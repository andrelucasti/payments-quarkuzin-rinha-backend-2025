package io.andrelucas.summary;


import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;

@Path("/payments-summary")
public class PaymentSummaryResource {

    private final GetSummary getSummary;

    public PaymentSummaryResource(GetSummary getSummary) {
        this.getSummary = getSummary;
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    public CompletableFuture<RestResponse<PaymentSummaryResponse>> summary(@QueryParam("from") Instant from,
                                                             @QueryParam("to") Instant to) {

        return getSummary.summary(from, to)
                .thenApply(RestResponse::ok);

    }
}
