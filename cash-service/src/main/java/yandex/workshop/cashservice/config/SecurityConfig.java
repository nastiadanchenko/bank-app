package yandex.workshop.cashservice.config;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
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

        http.oauth2ResourceServer(oauth2 ->
            oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
        );

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

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthorities);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return Collections.emptyList();
        }

        Object rolesObj = realmAccess.get("roles");

        // Проверяем, что roles — это коллекция (и не падаем, если Keycloak вернул что-то иное)
        if (!(rolesObj instanceof Collection<?> rawRoles)) {
            return Collections.emptyList();
        }

        var roles = rawRoles.stream()
            .filter(Objects::nonNull)
            .map(Object::toString)
            .toList();

        var authorities = roles.stream()
            .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toList());

        if (roles.contains("CASH_WRITE")) {
            authorities.add(new SimpleGrantedAuthority("cash.write"));
        }

        if (roles.contains("NOTIFICATIONS")) {
            authorities.add(new SimpleGrantedAuthority("notifications"));
        }

        return authorities;
    }
}