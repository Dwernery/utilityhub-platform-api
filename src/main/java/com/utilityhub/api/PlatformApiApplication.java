package com.utilityhub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Spring Boot would otherwise create a default "user" with a random password and
// print it on every start. Nothing here logs in with a password (requests carry the
// API token instead, see config/SecurityConfig), so that user would only mislead.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class PlatformApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApiApplication.class, args);
    }
}
