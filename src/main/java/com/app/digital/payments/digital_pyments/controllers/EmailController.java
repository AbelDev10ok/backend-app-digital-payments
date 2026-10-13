package com.app.digital.payments.digital_pyments.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.app.digital.payments.digital_pyments.models.dtos.EmailDto;
import com.app.digital.payments.digital_pyments.services.EmailServices;

import jakarta.mail.MessagingException;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/email")
public class EmailController {

    private final EmailServices emailServices;

    public EmailController(EmailServices emailServices) {
        this.emailServices = emailServices;
    }

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailDto entity) throws MessagingException {        
            emailServices.sendEmail(entity);
            return ResponseEntity.ok("Email sent successfully");

    }
    

}
