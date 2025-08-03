package io.andrelucas.payments;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/payments")
public class PaymentResource {

    private final PaymentProducer paymentProducer;

    public PaymentResource(PaymentProducer paymentProducer) {
        this.paymentProducer = paymentProducer;
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public RestResponse<Void> create(PaymentRequest paymentRequest) {

        paymentProducer.create(paymentRequest);

        return RestResponse.ok();
    }
}
