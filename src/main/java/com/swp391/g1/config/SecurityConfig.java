package com.swp391.g1.config;

import java.util.Arrays;
import java.util.Collection;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Temporary account source until the business account model is finalized.
     */
    @Bean
    public UserDetailsService userDetailsService(
            @Value("${app.auth.dev-account.username}") String devUsername,
            @Value("${app.auth.dev-account.password}") String devPasswordHash,
            @Value("${app.auth.dev-account.roles}") String devRoles) {
        return new InMemoryDevUserDetails(devUsername, devPasswordHash, devRoles);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                    .requestMatchers(
                        "/",
                        "/extracurricular-activities",
                        "/extracurricular-activities/**",
                        "/api/extracurricular-activities/**"
                    ).permitAll()
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .formLogin((formLogin) -> formLogin
                        .loginPage("/login")
                        .defaultSuccessUrl("/home")
                        .permitAll())
                .logout((logout) -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    private static final class InMemoryDevUserDetails implements UserDetailsService {

        private final String username;
        private final String passwordHash;
        private final Collection<? extends GrantedAuthority> authorities;

        InMemoryDevUserDetails(String username, String passwordHash, String roles) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.authorities = toAuthorities(roles);
        }

        @Override
        public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
            if (!this.username.equals(username)) {
                throw new UsernameNotFoundException("No such user: " + username);
            }
            return User.withUsername(this.username)
                    .password(this.passwordHash)
                    .authorities(this.authorities)
                    .build();
        }

        private static Collection<? extends GrantedAuthority> toAuthorities(String roles) {
            return Arrays.stream(roles.split(","))
                    .map(String::trim)
                    .filter((role) -> !role.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .toList();
        }
    }
}