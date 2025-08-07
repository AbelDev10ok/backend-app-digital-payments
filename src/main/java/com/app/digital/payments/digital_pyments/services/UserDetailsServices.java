package com.app.digital.payments.digital_pyments.services;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;

@Service
public class UserDetailsServices  implements UserDetailsService {

    @Autowired
    private IUsuarioRepository userRepository;

    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Usuario> user = userRepository.findByEmail(username);
        if(user.isEmpty()){
            throw new UsernameNotFoundException(String.format("User %s not found", username));
        }
        Usuario userDb = user.orElseThrow();
        
        // Get the single Role object from the user
        Role role = userDb.getRole();

        // Create a single-element list of GrantedAuthority using SimpleGrantedAuthority
        List<GrantedAuthority> authorities = Collections.singletonList(
        new SimpleGrantedAuthority(role.getName().toUpperCase())
    );
        // User de springSecurity
        return User.builder()
        .username(userDb.getEmail())
        .password(userDb.getPassword())
        .disabled(!userDb.isEnabled())
        .accountExpired(false)
        .accountLocked(false)
        .credentialsExpired(false)
        .authorities(authorities)
        .build();
    }
}
