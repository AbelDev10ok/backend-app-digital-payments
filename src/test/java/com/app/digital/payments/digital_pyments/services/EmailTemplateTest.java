package com.app.digital.payments.digital_pyments.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Los mails de verificacion y restablecimiento se renderizan con templates
 * Thymeleaf: tienen que traer el link como boton clickeable, usar la paleta de
 * Cobros&Ventas y no volver al texto blanco sobre fondo blanco del template
 * generico.
 */
class EmailTemplateTest {

    private static final String ENLACE = "http://localhost:5173/reset-password?token=tok-123";
    private static final String TEXTO_BOTON = "Crear contraseña nueva";

    private SpringTemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
    }

    private String renderizar(String template, String enlace, String textoBoton) {
        Context context = new Context();
        context.setVariable("enlace", enlace);
        context.setVariable("textoBoton", textoBoton);
        return templateEngine.process(template, context);
    }

    @Test
    void plantillaDeRestablecimientoEnlazaElLinkComoBoton() {
        String html = renderizar("restablecimiento", ENLACE, TEXTO_BOTON);

        assertTrue(html.contains("href=\"" + ENLACE + "\""),
                "el link tiene que quedar como href clickeable: " + html);
        assertTrue(html.contains(TEXTO_BOTON), "tiene que mostrar el texto del boton");
        assertTrue(html.contains("#1a8059"), "el boton usa el verde de marca de Cobros&Ventas");
        assertFalse(html.contains("style=\"color:#fff\""),
                "no puede volver al texto blanco sobre fondo blanco del template generico");
    }

    @Test
    void plantillaDeVerificacionEnlazaElLinkComoBoton() {
        String enlace = "http://localhost:5173/verify?token=tok-456";
        String html = renderizar("verificacion", enlace, "Verificar mi correo");

        assertTrue(html.contains("href=\"" + enlace + "\""),
                "el link tiene que quedar como href clickeable: " + html);
        assertTrue(html.contains("Verificar mi correo"), "tiene que mostrar el texto del boton");
        assertTrue(html.contains("#1a8059"), "el boton usa el verde de marca de Cobros&Ventas");
        assertFalse(html.contains("style=\"color:#fff\""),
                "no puede volver al texto blanco sobre fondo blanco del template generico");
    }

    @Test
    void losDosMailsMuestranElLinkEnTextoPorSiElBotonNoFunciona() {
        String verificacion = renderizar("verificacion", ENLACE, "Verificar mi correo");
        String restablecimiento = renderizar("restablecimiento", ENLACE, TEXTO_BOTON);

        assertTrue(verificacion.contains(ENLACE), "el link crudo tiene que estar visible");
        assertTrue(restablecimiento.contains(ENLACE), "el link crudo tiene que estar visible");
    }
}
