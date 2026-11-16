package com.app.digital.payments.digital_pyments.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.app.digital.payments.digital_pyments.models.Client;

class DireccionUtilsTest {

    @Test
    void uneLasPartesCargadasEnOrden() {
        assertEquals("Av. San Martín 1234, Rafael Calzada, Buenos Aires, B1847",
                DireccionUtils.formatear("Av. San Martín 1234", "Rafael Calzada", "Buenos Aires", "B1847"));
    }

    @Test
    void omiteLasPartesVaciasONulas() {
        assertEquals("Av. San Martín 1234",
                DireccionUtils.formatear("Av. San Martín 1234", null, "", "   "));
        assertEquals("Rafael Calzada, Buenos Aires",
                DireccionUtils.formatear(null, " Rafael Calzada ", "Buenos Aires", null));
    }

    @Test
    void devuelveNullSinNingunaParte() {
        assertNull(DireccionUtils.formatear(null, "", "  ", null));
    }

    @Test
    void formateaDesdeElCliente() {
        Client client = new Client();
        client.setDireccion("Calle Falsa 742");
        client.setLocalidad("Springfield");
        client.setProvincia("Buenos Aires");

        assertEquals("Calle Falsa 742, Springfield, Buenos Aires", DireccionUtils.formatear(client));
    }
}
