package com.neueda.orderservice.services.orderServices;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.neueda.orderservice.services.orderServices.OrderResult;

import org.junit.jupiter.api.Test;

public class OrderResultTest {

    @Test
    void testOrderResult() {
        OrderResult result = new OrderResult(true, "Order filled", null);
        assertTrue(result.isSuccess());
        assertEquals("Order filled", result.getMessage());
    }
}