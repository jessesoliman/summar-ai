package com.example.summar_ai.config;

import com.example.summar_ai.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.web.SecurityFilterChain;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class SecurityConfig {

    private AuthService authService;

    @Autowired
    @Lazy
    public void setAuthService(AuthService authService) {
        this.authService = authService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login", "/auth/register").permitAll()  // Allow access to login and registration pages
                        .anyRequest().authenticated()  // Secure other pages
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")  // Specify custom login page
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()  // Allow everyone to access the login page
                )
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession()   // Keeps session but migrates attributes
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS))
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/oauth2/success", true)  // Redirect to success page after OAuth2 login
                        .authorizationEndpoint(authorization -> authorization
                                .baseUri("/oauth2/authorization")  // Authorization endpoint for OAuth2 login
                                .authorizationRequestResolver(customAuthorizationRequestResolver(clientRegistrationRepository)) // Custom resolver for refresh token
                        )
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public DefaultOAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        DefaultOAuth2AuthorizationRequestResolver resolver = new DefaultOAuth2AuthorizationRequestResolver(clientRegistrationRepository, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer -> {
            Map<String, Object> additionalParameters = new HashMap<>(customizer.build().getAdditionalParameters());

            // Add parameters specific to Jira/Atlassian
            if (customizer.build().getAuthorizationUri().contains("atlassian")) {
                additionalParameters.put("audience", "api.atlassian.com");
                additionalParameters.put("prompt", "consent");
            }

            // Add parameters for Google to get refresh tokens
            if (customizer.build().getAuthorizationUri().contains("google")) {
                additionalParameters.put("access_type", "offline");
            }

            customizer.additionalParameters(additionalParameters);
        });
        return resolver;
    }
}
