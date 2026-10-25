package com.app.digital.payments.digital_pyments.models.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

// El nombre del campo es parte del contrato con el frontend: Jackson lo deriva
// del getter, y `getACobrar()` se convertía en "acobrar".
class SalesCountsDtoTest {

    @Test
    void serializaACobrarConElNombreQueEsperaElFrontend() throws Exception {
        SalesCountsDto counts = new SalesCountsDto();
        counts.setACobrar(4);

        String json = new ObjectMapper().writeValueAsString(counts);

        assertTrue(json.contains("\"aCobrar\":4"), json);
        assertFalse(json.contains("acobrar"), json);
    }

    @Test
    void deserializaACobrarSinPerderElValor() throws Exception {
        SalesCountsDto counts = new ObjectMapper()
                .readValue("{\"aCobrar\":7}", SalesCountsDto.class);

        assertEquals(7, counts.getACobrar());
    }
}
