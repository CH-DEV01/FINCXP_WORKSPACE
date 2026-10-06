package com.davivienda.factoraje.infrastructure.config;

import com.davivienda.factoraje.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Configuration
@RequiredArgsConstructor
public class ApplicationConfig {

    private final UserRepository userRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        // Le indicamos que el "username" de Spring es nuestro DUI
        return dui -> userRepository.findByDui(dui)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con DUI: " + dui));
    }
}
