package com.app.digital.payments.digital_pyments.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.digital.payments.digital_pyments.models.Role;
import com.app.digital.payments.digital_pyments.models.Usuario;
import com.app.digital.payments.digital_pyments.models.dtos.UsuarioAuth;
import com.app.digital.payments.digital_pyments.models.dtos.UsuarioResponse;
import com.app.digital.payments.digital_pyments.repositories.IRoleRepository;
import com.app.digital.payments.digital_pyments.repositories.IUsuarioRepository;

import io.jsonwebtoken.security.Password;

@Service
public class UsuarioServices {

    @Autowired
    private IUsuarioRepository usuarioRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional(readOnly=true)
    public List<UsuarioResponse> getAllUsers() {
        return usuarioRepository.findAll().stream()
                .map(user -> 
                    {
                        UsuarioResponse response = new UsuarioResponse();
                        response.setId(user.getId());
                        response.setEmail(user.getEmail());
                        response.setRole(user.getRole());
                        response.setEnabled(user.isEnabled());
                        return response;
                    }
                
                ) // Usa ModelMapper aquí
                .toList();
    }
    
    @Transactional(readOnly=true)
    public Usuario getUsersById(Long id) {
        return usuarioRepository.findById(id).get();
    }

    public Usuario findByEmail(String email){
        return usuarioRepository.findByEmail(email).get();
    }


    @Transactional
    public void saveUser(UsuarioAuth user) {
        boolean exists = usuarioRepository.existsByEmail(user.getEmail());
        if(exists){
            throw new RuntimeException("User already exists with email: " + user.getEmail());
        }
        Optional<Role> roleUser = roleRepository.findByName("ROLE_USER");   
        
        if(roleUser.isEmpty()){
            throw new RuntimeException("Role not found: ROLE_USER");
        }

        Role role = roleUser.get();

        Usuario userDb = new Usuario();
        userDb.setEmail(user.getEmail());
        userDb.setPassword(passwordEncoder.encode(user.getPassword()));
        userDb.setRole(role);

        usuarioRepository.save(userDb);
    }

        
    @Transactional
    public void deleteUserById(Long id) {
        Optional<Usuario> user = usuarioRepository.findById(id);
        if(!user.isPresent()){
            throw new RuntimeException("User not found");
        }
        usuarioRepository.deleteById(id);
    }


    @Transactional
    public void disableUser(String email, boolean enabled) {
        Optional<Usuario> userOptional = usuarioRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            Usuario user = userOptional.get();
            user.setEnabled(enabled);
            usuarioRepository.save(user);
        } else {
            throw new RuntimeException("User not found");
        }
    }

    public boolean existsUser(String email) {
        return usuarioRepository.existsByEmail(email);
    }


}
