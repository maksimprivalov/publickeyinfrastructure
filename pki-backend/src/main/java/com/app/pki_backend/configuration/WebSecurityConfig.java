package com.app.pki_backend.configuration;

import com.app.pki_backend.security.auth.RestAuthenticationEntryPoint;
import com.app.pki_backend.security.auth.TokenAuthenticationFilter;
import com.app.pki_backend.service.external.CustomUserDetailsService;
import com.app.pki_backend.util.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity(debug = true)
@EnableMethodSecurity
public class WebSecurityConfig{

    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Autowired
    private TokenUtils tokenUtils;
    @Bean
    public UserDetailsService userDetailsService() {
        return new CustomUserDetailsService();
    }
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService());
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(Customizer.withDefaults());
        http.csrf((csrf) -> csrf.disable());
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(restAuthenticationEntryPoint));
        http.authorizeHttpRequests(request -> {
            request.requestMatchers("/api/users/login").permitAll()
                    .requestMatchers("/api/users/register").permitAll()
                    .requestMatchers("/api/users/activate").permitAll()
                    .requestMatchers("/api/users/logout").authenticated()

                    // Certificates
                    .requestMatchers(HttpMethod.GET, "/api/certificates").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/certificates/*").authenticated()
                    .requestMatchers(HttpMethod.DELETE, "/api/certificates/*").hasRole("ADMIN")

                    .requestMatchers("/api/certificates/issue/root").hasRole("ADMIN")
                    .requestMatchers("/api/certificates/issue/intermediate/**").hasAnyRole("ADMIN","CAUSER")
                    .requestMatchers("/api/certificates/issue/ee/**").hasAnyRole("ADMIN","CAUSER")

                    .requestMatchers("/api/certificates/issue/root/template/**").hasRole("ADMIN")
                    .requestMatchers("/api/certificates/issue/intermediate/template/**").hasAnyRole("ADMIN","CAUSER")
                    .requestMatchers("/api/certificates/issue/ee/template/**").hasAnyRole("ADMIN","CAUSER")

                    // Templates
                    .requestMatchers(HttpMethod.POST, "/api/certificates/templates").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/certificates/templates").hasAnyRole("ADMIN","CAUSER")
                    .requestMatchers(HttpMethod.DELETE, "/api/certificates/templates/*").hasRole("ADMIN")

                    // Download + CSR
                    .requestMatchers(HttpMethod.GET, "/api/certificates/*/download").authenticated()
                    .requestMatchers(HttpMethod.POST, "/api/certificates/csr/upload/**").hasRole("USER")

                    // Search
                    .requestMatchers(HttpMethod.GET, "/api/certificates/search").hasAnyRole("ADMIN","CAUSER")

                    // Revocations
                    .requestMatchers(HttpMethod.POST, "/api/revocations/*/revoke").hasAnyRole("ADMIN","CAUSER")
                    .requestMatchers(HttpMethod.GET, "/api/revocations").hasAnyRole("ADMIN","CAUSER")
                    .requestMatchers(HttpMethod.GET, "/api/revocations/crl").hasAnyRole("ADMIN","CAUSER")

                    // Error
                    .requestMatchers("/error").permitAll()


                    .anyRequest().authenticated();
        });
        http.logout(logout -> logout.disable());
        http.addFilterBefore(new TokenAuthenticationFilter(tokenUtils, userDetailsService()), UsernamePasswordAuthenticationFilter.class);
        http.authenticationProvider(authenticationProvider());
        return http.build();
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers(HttpMethod.GET, "/", "/webjars/*", "/*.html", "favicon.ico",
                        "/*/*.html", "/*/*.css", "/*/*.js");
    }
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(
                "https://localhost:5173",
                "https://192.168.1.3:8000",
                "http://localhost:5173",
                "http://192.168.1.3:8000"
        ));
        configuration.setAllowedMethods(Arrays.asList("POST", "PUT", "GET", "OPTIONS", "DELETE", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


}
