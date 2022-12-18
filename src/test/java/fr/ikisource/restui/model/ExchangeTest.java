package fr.ikisource.restui.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import fr.ikisource.restui.model.Parameter.Direction;
import fr.ikisource.restui.model.Parameter.Location;
import fr.ikisource.restui.model.Parameter.Type;

class ExchangeTest {

    @Test
    void requestHeaders() {

        Exchange exchange = new Exchange("e1", 1L);
        Parameter parameter1 = new Parameter(true, Direction.REQUEST, Location.HEADER, Type.TEXT, "p1", "10");
        Parameter parameter2 = new Parameter(true, Direction.REQUEST, Location.HEADER, Type.TEXT, "p2", "20");
        exchange.addParameter(parameter1);
        exchange.addParameter(parameter2);

        String[] requestHeaders = exchange.requestHeaders();
        System.out.println(requestHeaders);

        assertNotNull(requestHeaders);
        assertEquals(4, requestHeaders.length);
        assertEquals("p1", requestHeaders[0]);
        assertEquals("10", requestHeaders[1]);
        assertEquals("p2", requestHeaders[2]);
        assertEquals("20", requestHeaders[3]);

        exchange.getParameters().clear();
        requestHeaders = exchange.requestHeaders();
        assertNotNull(requestHeaders);
        assertEquals(0, requestHeaders.length);
    }

}