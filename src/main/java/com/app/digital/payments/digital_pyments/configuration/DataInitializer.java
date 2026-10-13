package com.app.digital.payments.digital_pyments.configuration;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.app.digital.payments.digital_pyments.models.Business;
import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.repositories.IBusinessRepository;
import com.app.digital.payments.digital_pyments.repositories.IRoleRepository;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final IUsuarioRepository userRepo;
    private final IRoleRepository roleRepo;
    private final IBusinessRepository businessRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(
            IUsuarioRepository userRepo,
            IRoleRepository roleRepo,
            IBusinessRepository businessRepo,
            PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.businessRepo = businessRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = "TESTUSER4559908250374305203@testuser.com";
        String rawPassword = "prueba";

        Role role = roleRepo.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("ROLE_USER no existe en BD"));

        Usuario existingUser = userRepo.findByEmail(email).orElse(null);
        if (existingUser != null) {
            existingUser.setPassword(encoder.encode(rawPassword));
            existingUser.setEnabled(true);
            userRepo.save(existingUser);
            log.info("Usuario de prueba actualizado: {}", email);
            return;
        }

        Business business = new Business();
        business.setName("Negocio de Prueba");
        business.setPlanStatus("TRIAL");
        business.setTrialEndsAt(LocalDate.now().plusDays(30));
        businessRepo.save(business);

        Usuario user = new Usuario();
        user.setEmail(email);
        user.setPassword(encoder.encode(rawPassword));
        user.setEnabled(true);
        user.setRole(role);
        user.setBusiness(business);
        userRepo.save(user);

        log.info("Usuario de prueba creado: {}", email);
    }
}
