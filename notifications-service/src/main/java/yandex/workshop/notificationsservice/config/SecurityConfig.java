package yandex.workshop.notificationsservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain transferSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/actuator/**").permitAll();
            auth.anyRequest().authenticated();
        });

        http.exceptionHandling(exception -> exception
            .accessDeniedHandler((request, response, accessDeniedException) -> {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write(
                    accessDeniedException.getMessage() != null
                        ? accessDeniedException.getMessage()
                        : "Доступ запрещён"
                );
            })
        );

        return http.build();
    }

}
