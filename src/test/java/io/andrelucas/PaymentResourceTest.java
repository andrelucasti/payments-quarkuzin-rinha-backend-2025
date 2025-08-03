package io.andrelucas;

import io.andrelucas.payments.PaymentRequest;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
class PaymentResourceTest {

    @Test
    public void testPayment() {
        PaymentRequest paymentRequest = new PaymentRequest(UUID.randomUUID(), BigDecimal.valueOf(19.90));
        given()
                .contentType(ContentType.JSON)
                .body(paymentRequest)
                .when().post("/payments")
                .then()
                .statusCode(200);
    }
}