package com.app.digital.payments.digital_pyments.utils;

import java.util.ArrayList;
import java.util.List;

import com.app.digital.payments.digital_pyments.models.Client;

/**
 * Compone el domicilio del cliente en una sola linea, omitiendo las partes
 * vacias. Es el unico lugar donde se arma la direccion que consumen la agenda
 * de cobros (ruta en Maps), el remito y los listados: si se compone en cada
 * consumidor, terminan mostrando direcciones distintas.
 */
public final class DireccionUtils {

    private DireccionUtils() {
    }

    public static String formatear(String direccion, String localidad, String provincia, String codigoPostal) {
        List<String> partes = new ArrayList<>();
        agregar(partes, direccion);
        agregar(partes, localidad);
        agregar(partes, provincia);
        agregar(partes, codigoPostal);
        return partes.isEmpty() ? null : String.join(", ", partes);
    }

    public static String formatear(Client client) {
        if (client == null) {
            return null;
        }
        return formatear(client.getDireccion(), client.getLocalidad(), client.getProvincia(),
                client.getCodigoPostal());
    }

    private static void agregar(List<String> partes, String valor) {
        if (valor != null && !valor.isBlank()) {
            partes.add(valor.trim());
        }
    }
}
