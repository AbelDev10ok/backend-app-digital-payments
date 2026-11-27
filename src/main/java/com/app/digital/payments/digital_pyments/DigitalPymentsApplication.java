package com.app.digital.payments.digital_pyments;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DigitalPymentsApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigitalPymentsApplication.class, args);
	}

}
