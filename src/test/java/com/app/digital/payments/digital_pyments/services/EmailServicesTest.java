package com.app.digital.payments.digital_pyments.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.app.digital.payments.digital_pyments.models.dtos.EmailDto;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

class EmailServicesTest {

    private JavaMailSender mailSender;

    private TemplateEngine templateEngine;

    private EmailServices emailServices;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        templateEngine = mock(TemplateEngine.class);
        emailServices = new EmailServices(mailSender, templateEngine);
    }

    @Test
    void sendEmailEnviaMensaje() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html><body>Hola</body></html>");

        EmailDto dto = new EmailDto();
        dto.setDestinatario("destino@example.com");
        dto.setAsunto("Asunto de prueba");
        dto.setMensaje("Mensaje de prueba");

        emailServices.sendEmail(dto);

        verify(mailSender).send(any(MimeMessage.class));
    }
}
