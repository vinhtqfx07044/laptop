package com.laptoprepair.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

        @Value("${app.security.login.default-success-url}")
        private String defaultSuccessUrl;

        private final Environment environment;

        public SecurityConfig(Environment environment) {
                this.environment = environment;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        private static final String[] ANONYMOUS_ONLY = {
                        "/about", "/lookup", "/submit", "/recover"
        };

        public static final String LOGIN_PATH = "/login";

        private static final String[] PERMIT_ALL = {
                        "/", "/h2-console/**", LOGIN_PATH, "/logout", "/error",
                        "/css/**", "/js/**", "/favicon.ico",
                        "/images/**",
                        "/public/**",
                        "/api/chat/**",
                        "/actuator/**",
        };

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PERMIT_ALL).permitAll()
                                                .requestMatchers(ANONYMOUS_ONLY).anonymous()
                                                .requestMatchers("/staff/**").hasRole("STAFF")
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage(LOGIN_PATH)
                                                .loginProcessingUrl(LOGIN_PATH)
                                                .defaultSuccessUrl(defaultSuccessUrl, true)
                                                .failureUrl("/login?error")
                                                .permitAll())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(
                                                                (request, response, authException) -> response
                                                                                .sendRedirect(LOGIN_PATH)))
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/")
                                                .permitAll())
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers("/h2-console/**", "/api/chat/**"))
                                .headers(headers -> headers
                                                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                                                .contentSecurityPolicy(csp -> csp.policyDirectives(
                                                                "default-src 'self'; " +
                                                                                "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; "
                                                                                +
                                                                                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; "
                                                                                +
                                                                                "font-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; "
                                                                                +
                                                                                "img-src 'self' data:; " +
                                                                                "frame-src 'self' https://www.google.com; "
                                                                                +
                                                                                "connect-src 'self'"))
                                                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                                                .maxAgeInSeconds(31536000)))
                                .build();
        }

        @Bean
        public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
                String usersConfig = environment.getProperty("app.security.staff.users");
                List<UserDetails> users = new ArrayList<>();

                if (usersConfig != null && !usersConfig.isEmpty()) {
                        for (String userConfig : usersConfig.split(",")) {
                                String[] parts = userConfig.trim().split(":");
                                if (parts.length == 2) {
                                        String username = parts[0].trim();
                                        String rawPassword = parts[1].trim();
                                        String encodedPassword = passwordEncoder.encode(rawPassword);

                                        UserDetails user = User.builder()
                                                        .username(username)
                                                        .password(encodedPassword)
                                                        .roles("STAFF")
                                                        .build();
                                        users.add(user);
                                } else {
                                        logger.warn("Invalid user config format: {}", userConfig);
                                }
                        }
                } else {
                        logger.warn("No STAFF_USERS configuration found - no users will be created");
                }

                logger.info("Created {} users for authentication", users.size());
                return new InMemoryUserDetailsManager(users);
        }

}
