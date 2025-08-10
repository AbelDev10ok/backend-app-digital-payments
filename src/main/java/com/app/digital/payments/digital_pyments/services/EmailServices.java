package com.app.digital.payments.digital_pyments.services;

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
            e.printStackTrace();
            throw new MessagingException("Error al enviar el correo electrónico: " + e.getMessage());
        }

    }
}
