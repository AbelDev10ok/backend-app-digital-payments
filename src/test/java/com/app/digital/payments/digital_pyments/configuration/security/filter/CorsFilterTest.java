package com.app.digital.payments.digital_pyments.configuration.security.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * CORS configurable por propiedad: los origenes salen de app.cors.allowed-origins
 * (lista separada por comas con espacios opcionales) y no de una constante.
 * Regresion de la lista hardcodeada, que en produccion bloqueaba al frontend real.
 */
class CorsFilterTest {

    private static MockHttpServletRequest pedido(String metodo, String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, "/api/sales");
        if (origin != null) {
            request.addHeader("Origin", origin);
        }
        return request;
    }

    @Test
    void permiteUnOrigenConfiguradoYRecortaLosEspaciosDeLaLista() throws Exception {
        CorsFilter filter = new CorsFilter("https://app.ejemplo.com, http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(pedido("GET", "https://app.ejemplo.com"), response, new MockFilterChain());

        assertEquals("https://app.ejemplo.com", response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        assertEquals("Origin", response.getHeader("Vary"));
    }

    @Test
    void noHabilitaUnOrigenQueNoEstaEnLaLista() throws Exception {
        CorsFilter filter = new CorsFilter("https://app.ejemplo.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(pedido("GET", "https://malicioso.example"), response, new MockFilterChain());

        assertNull(response.getHeader("Access-Control-Allow-Origin"));
    }

    @Test
    void respondeElPreflightDeUnOrigenPermitidoSinSeguirLaCadena() throws Exception {
        CorsFilter filter = new CorsFilter("http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(pedido("OPTIONS", "http://localhost:5173"), response, chain);

        assertEquals(200, response.getStatus());
        assertEquals("GET,POST,PUT,DELETE,PATCH", response.getHeader("Access-Control-Allow-Methods"));
        assertEquals("Authorization,Content-Type,X-Requested-With",
                response.getHeader("Access-Control-Allow-Headers"));
        assertNull(chain.getRequest());
    }

    @Test
    void dejaPasarLosPedidosSinOrigen() throws Exception {
        CorsFilter filter = new CorsFilter("http://localhost:5173");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(pedido("GET", null), response, chain);

        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertNotNull(chain.getRequest());
    }

    @Test
    void ignoraEntradasVaciasYComasRepetidas() throws Exception {
        CorsFilter filter = new CorsFilter(" , https://a.ejemplo.com ,, ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(pedido("GET", "https://a.ejemplo.com"), response, new MockFilterChain());

        assertEquals("https://a.ejemplo.com", response.getHeader("Access-Control-Allow-Origin"));

        MockHttpServletResponse responseSinOrigen = new MockHttpServletResponse();
        filter.doFilter(pedido("GET", ""), responseSinOrigen, new MockFilterChain());

        assertNull(responseSinOrigen.getHeader("Access-Control-Allow-Origin"));
    }
}
