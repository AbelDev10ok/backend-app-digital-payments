package com.app.digital.payments.digital_pyments.configuration;

import java.time.LocalDate;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IRoleRepository;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;

import jakarta.annotation.PostConstruct;

@Configuration
@PropertySource("classpath:email.properties") // 1. Indica a Spring que lea este archivo de propiedades
public class AppCong {



    // 2. Inyecta los valores desde el archivo .properties
    @Value("${email.auth.username}")
    private String username;

    @Value("${email.auth.password}")
    private String password;

    @Bean 
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }


    @Bean
    public JavaMailSender javaMailSender() {

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // awfd updc goas iurc

        mailSender.setHost("smtp.gmail.com"); // O el host de tu proveedor de correo
        mailSender.setPort(587); // O el puerto correcto para tu proveedor
        mailSender.setUsername("abel.alarcon.dev@gmail.com"); // Tu dirección de correo
        mailSender.setPassword("awfd updc goas iurc"); // Contraseña de aplicación para Gmail
        // mailSender.setUsername(System.getenv("EMAIL_USERNAME")); // Tu dirección de correo
        // mailSender.setPassword(System.getenv("EMAIL_PASSWORD")); //  Contraseña de aplicación para Gmail
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "false");

        //Si usas SSL en lugar de TLS:
        //props.put("mail.smtp.ssl.enable", "true");
        //props.put("mail.smtp.socketFactory.port", "465");
        //props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        return mailSender;
    }




}
