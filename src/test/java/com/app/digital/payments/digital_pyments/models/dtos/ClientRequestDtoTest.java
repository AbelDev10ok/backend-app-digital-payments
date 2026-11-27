package com.app.digital.payments.digital_pyments.models.dtos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * El teléfono del cliente se guarda en E.164 argentino (+549 + área + número):
 * el frontend normaliza y manda el "+", y los teléfonos legados que no se
 * pudieron interpretar quedan sin prefijo. La validación tiene que aceptar los
 * dos y seguir rechazando el formato humano ("11 5555-1234"), que el frontend
 * nunca manda porque lo normaliza antes.
 */
class ClientRequestDtoTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private ClientRequestDto clienteConTelefono(String telefono) {
        ClientRequestDto dto = new ClientRequestDto();
        dto.setName("Cliente Test");
        dto.setDni("99888777");
        dto.setTelefono(telefono);
        return dto;
    }

    /** Mensajes de validación del campo teléfono: los demás campos van válidos. */
    private Set<String> erroresDeTelefono(String telefono) {
        return validator.validate(clienteConTelefono(telefono)).stream()
                .filter(violacion -> "telefono".equals(violacion.getPropertyPath().toString()))
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void aceptaElTelefonoEnE164QueMandaElFrontend() {
        assertTrue(erroresDeTelefono("+5491155551234").isEmpty(),
                "el alta de cliente manda +549... y el backend respondía 400");
    }

    @Test
    void aceptaElTelefonoNacionalLegado() {
        assertTrue(erroresDeTelefono("1155551234").isEmpty());
    }

    @Test
    void sigueRechazandoElFormatoHumanoConEspacios() {
        assertEquals(1, erroresDeTelefono("11 5555-1234").size());
    }

    @Test
    void sigueRechazandoUnTelefonoDemasiadoCorto() {
        assertEquals(1, erroresDeTelefono("115555123").size());
    }
}
