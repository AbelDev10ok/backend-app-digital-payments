package com.app.digital.payments.digital_pyments.utils;

import java.text.Normalizer;
import java.util.Locale;

/** Utilidades de slugs: minúsculas, sin acentos y separados por guiones. */
public final class SlugUtils {

    private SlugUtils() {
    }

    public static String slugify(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
    }
}
