package com.laptoprepair.config;

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
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

        @Value("${app.security.login.default-success-url}")
        private String defaultSuccessUrl;

        private final Environment environment;

        public SecurityConfig(Environment environment) {
                this.environment = environment;
        }

        // Chỉ cho anonymous truy cập (redirect staff)
        private static final String[] ANONYMOUS_ONLY = {
                        "/about", "/lookup", "/submit", "/recover"
        };

        // Cho phép tất cả truy cập
        public static final String LOGIN_PATH = "/login";

        // Cho phép tất cả truy cập
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
                                .csrf(csrf -> csrf.disable())
                                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()))
                                .build();
        }

        @Bean
        public UserDetailsService userDetailsService() {
                String usersConfig = environment.getProperty("app.security.staff.users");
                List<UserDetails> users = new ArrayList<>();

                if (usersConfig != null && !usersConfig.isEmpty()) {
                        for (String userConfig : usersConfig.split(",")) {
                                String[] parts = userConfig.trim().split(":");
                                if (parts.length == 2) {
                                        UserDetails user = User.builder()
                                                        .username(parts[0].trim())
                                                        .password("{noop}" + parts[1].trim())
                                                        .roles("STAFF")
                                                        .build();
                                        users.add(user);
                                }
                        }
                }

                return new InMemoryUserDetailsManager(users);
        }

}
