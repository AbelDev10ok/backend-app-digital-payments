package com.app.digital.payments.digital_pyments.controllers;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private EmailServices emailServices;
    

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailDto entity) throws MessagingException {        
            emailServices.sendEmail(entity);
            return ResponseEntity.ok("Email sent successfully");

    }
    

}
