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

/**
 * Spring Security configuration for the v1.0 authentication foundation.
 *
 * <p>The account source is intentionally a temporary in-memory
 * {@link UserDetailsService} because the business User/Account model has not
 * been finalized yet - there is deliberately no entity, table or repository.
 * The development credentials are configuration-backed
 * ({@code app.auth.dev-account.*}); the password is stored as a BCrypt hash
 * and is never compared as plain text. This placeholder must be replaced by
 * the final business account store in a later phase.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * BCrypt encoder used to verify the development account credentials.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Temporary in-memory {@link UserDetailsService} backed by configuration.
     *
     * @param devUsername     configured development username
     * @param devPasswordHash configured BCrypt-encoded development password
     * @param devRoles        configured authorities (kept minimal; no roles in v1.0)
     */
    @Bean
    public UserDetailsService userDetailsService(
            @Value("${app.auth.dev-account.username}") String devUsername,
            @Value("${app.auth.dev-account.password}") String devPasswordHash,
            @Value("${app.auth.dev-account.roles}") String devRoles) {
        return new InMemoryDevUserDetails(devUsername, devPasswordHash, devRoles);
    }

    /**
     * Builds the security filter chain: URL-level access rules, form login and
     * logout. CSRF stays enabled (default) - it is never disabled.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
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

    /**
     * Temporary single-account {@link UserDetailsService} until the business
     * account model is finalized.
     */
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
            // The configured value is the pre-encoded BCrypt hash; the
            // DaoAuthenticationProvider verifies it with the BCrypt encoder, so
            // plain-text passwords are never stored.
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