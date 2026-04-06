package com.collab.apigateway.security;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    // In-memory user store - in production, this would be a database
    private final Map<String, UserDetails> users = new HashMap<>();

    public UserDetailsServiceImpl(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;

        // Create some demo users with encoded passwords
        users.put("demo", User.withUsername("demo")
                .password(passwordEncoder.encode("password"))
                .roles("USER")
                .build());

        users.put("admin", User.withUsername("admin")
                .password(passwordEncoder.encode("admin123"))
                .roles("ADMIN", "USER")
                .build());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return user;
    }

    public void addUser(String username, String password, String... roles) {
        users.put(username, User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles(roles)
                .build());
    }
}