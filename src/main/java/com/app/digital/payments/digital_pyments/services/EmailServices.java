package com.app.digital.payments.digital_pyments.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.app.digital.payments.digital_pyments.models.dtos.EmailDto;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailServices {

    private static final Logger log = LoggerFactory.getLogger(EmailServices.class);

    private final JavaMailSender mailSender;

    private final TemplateEngine templateEngine;

    public EmailServices(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }


    // metodo para enviar email
    public void sendEmail(EmailDto email) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email.getDestinatario());
            helper.setSubject(email.getAsunto());

            Context context = new Context();
            context.setVariable("mensaje", email.getMensaje());
            // email template de resources/templates/email.html
            String htmlContent = templateEngine.process("email", context);
            helper.setText(htmlContent, true); // true para indicar que es HTML
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error al enviar el correo a {}", email.getDestinatario(), e);
            throw new MessagingException("Error al enviar el correo electrónico: " + e.getMessage());
        }

    }

    /**
     * Mail de verificacion de correo al registrarse: el link abre la pagina del
     * frontend que confirma la cuenta.
     */
    public void sendVerificationEmail(String destinatario, String verificationLink) throws MessagingException {
        sendTemplatedEmail(destinatario, "Verificá tu correo · Cobros&Ventas", "verificacion",
                verificationLink, "Verificar mi correo");
    }

    /**
     * Mail de restablecimiento de contraseña: el link abre la pagina del frontend
     * donde se elige la contraseña nueva.
     */
    public void sendPasswordResetEmail(String destinatario, String resetLink) throws MessagingException {
        sendTemplatedEmail(destinatario, "Restablecé tu contraseña · Cobros&Ventas", "restablecimiento",
                resetLink, "Crear contraseña nueva");
    }

    /**
     * Envia un mail con uno de los templates de resources/templates: el link
     * queda como boton clickeable y tambien visible en texto plano, por si el
     * cliente de correo no muestra botones.
     */
    private void sendTemplatedEmail(String destinatario, String asunto, String template,
            String enlace, String textoBoton) throws MessagingException {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(destinatario);
            helper.setSubject(asunto);

            Context context = new Context();
            context.setVariable("enlace", enlace);
            context.setVariable("textoBoton", textoBoton);

            String htmlContent = templateEngine.process(template, context);
            helper.setText(htmlContent, true); // true para indicar que es HTML
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Error al enviar el correo a {}", destinatario, e);
            throw new MessagingException("Error al enviar el correo electrónico: " + e.getMessage());
        }
    }
}
