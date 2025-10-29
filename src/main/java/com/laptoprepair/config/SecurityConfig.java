package com.laptoprepair.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

        private final Environment environment;

        // URL paths
        private static final String DEFAULT_SUCCESS_URL = "/staff/requests/list";
        private static final String LOGIN_FAILURE_URL = "/login?error";
        private static final String LOGOUT_URL = "/logout";
        private static final String LOGOUT_SUCCESS_URL = "/";
        private static final String H2_CONSOLE_PATH = "/h2-console/**";
        private static final String CHAT_API_PATH = "/api/chat/**";
        private static final String STAFF_PATH_PATTERN = "/staff/**";

        // Security configuration
        public static final String LOGIN_PATH = "/login";
        private static final String STAFF_ROLE = "STAFF";
        private static final String STAFF_USERS_PROPERTY = "laptoprepair.security.staff-users";

        // Message templates
        private static final String INVALID_USER_CONFIG_MSG = "Invalid user config format: {}";
        private static final String NO_STAFF_USERS_CONFIG_MSG = "No STAFF_USERS configuration found - no users will be created";
        private static final String USERS_CREATED_MSG = "Created {} users for authentication";

        // Path arrays
        private static final String[] ANONYMOUS_ONLY = {
                        "/about", "/lookup", "/submit", "/recover", "/documents/**"
        };

        private static final String[] PERMIT_ALL = {
                        "/", H2_CONSOLE_PATH, LOGIN_PATH, LOGOUT_URL, "/error",
                        "/css/**", "/js/**", "/favicon.ico",
                        "/images/**",
                        "/public/**",
                        CHAT_API_PATH,
                        "/actuator/**",
        };

        private static final String CONTENT_SECURITY_POLICY = "default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                        "font-src 'self' https://cdn.jsdelivr.net https://cdnjs.cloudflare.com; " +
                        "img-src 'self' data:; " +
                        "frame-src 'self' https://www.google.com; " +
                        "connect-src 'self'";

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                return http
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(PERMIT_ALL).permitAll()
                                                .requestMatchers(ANONYMOUS_ONLY).anonymous()
                                                .requestMatchers(STAFF_PATH_PATTERN).hasRole(STAFF_ROLE)
                                                .anyRequest().authenticated())
                                .formLogin(form -> form
                                                .loginPage(LOGIN_PATH)
                                                .loginProcessingUrl(LOGIN_PATH)
                                                .defaultSuccessUrl(DEFAULT_SUCCESS_URL, true)
                                                .failureUrl(LOGIN_FAILURE_URL)
                                                .permitAll())
                                .exceptionHandling(ex -> ex
                                                .authenticationEntryPoint(
                                                                (request, response, authException) -> response
                                                                                .sendRedirect(LOGIN_PATH)))
                                .logout(logout -> logout
                                                .logoutUrl(LOGOUT_URL)
                                                .logoutSuccessUrl(LOGOUT_SUCCESS_URL)
                                                .permitAll())
                                .csrf(csrf -> csrf
                                                .ignoringRequestMatchers(H2_CONSOLE_PATH, CHAT_API_PATH))
                                .headers(headers -> headers
                                                .frameOptions(f -> f.sameOrigin())
                                                .contentSecurityPolicy(
                                                                csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                                                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                                                                .maxAgeInSeconds(31536000)))
                                .build();
        }

        @Bean
        public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
                String usersConfig = environment.getProperty(STAFF_USERS_PROPERTY);
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
                                                        .roles(STAFF_ROLE)
                                                        .build();
                                        users.add(user);
                                } else {
                                        logger.warn(INVALID_USER_CONFIG_MSG, userConfig);
                                }
                        }
                } else {
                        logger.warn(NO_STAFF_USERS_CONFIG_MSG);
                }

                logger.info(USERS_CREATED_MSG, users.size());
                return new InMemoryUserDetailsManager(users);
        }

}
