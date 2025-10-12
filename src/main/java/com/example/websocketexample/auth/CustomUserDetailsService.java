package com.example.websocketexample.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final PasswordEncoder passwordEncoder;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Для демонстрации создаем пользователя в памяти
        if ("user".equals(username)) {
            return User.builder()
                    .username(username)
                    .password(passwordEncoder.encode("password"))
                    .roles("USER")
                    .build();
        } else if ("angelina".equals(username)) {
            return User.builder()
                    .username(username)
                    .password(passwordEncoder.encode("angelina"))
                    .roles("DEVELOPER")
                    .build();
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }
}
