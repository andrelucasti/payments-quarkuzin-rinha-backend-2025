package io.andrelucas.payments;

import io.quarkus.virtual.threads.VirtualThreads;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

import java.util.concurrent.Executor;

@Path("/payments")
public class PaymentResource {

    private final PaymentProducer paymentProducer;
    private final Executor executor;

    public PaymentResource(final PaymentProducer paymentProducer,
                           @VirtualThreads final Executor executor) {

        this.paymentProducer = paymentProducer;
        this.executor = executor;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestResponse<Void> create(PaymentRequest paymentRequest) {

        executor.execute(() -> paymentProducer.create(paymentRequest));

        return RestResponse.ok();
    }
}
