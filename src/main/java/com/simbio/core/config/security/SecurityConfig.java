package com.simbio.core.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.header.writers.StaticHeadersWriter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;

import com.simbio.core.config.observability.CorrelationIdFilter;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    CspNonceFilter cspNonceFilter(Environment environment) {
        return new CspNonceFilter(environment);
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(RateLimitFilter rateLimitFilter) {
        FilterRegistrationBean<RateLimitFilter> registration = new FilterRegistrationBean<>(rateLimitFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<AdminMfaFilter> adminMfaFilterRegistration(AdminMfaFilter filter) {
        FilterRegistrationBean<AdminMfaFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Environment environment,
            CorrelationIdFilter correlationIdFilter,
            CspNonceFilter cspNonceFilter,
            RateLimitFilter rateLimitFilter,
            AdminMfaFilter adminMfaFilter,
            SessionRegistry sessionRegistry,
            AuthenticationFailureHandler authenticationFailureHandler,
            AuthenticationSuccessHandler authenticationSuccessHandler)
            throws Exception {
        boolean secureCookies = environment.matchesProfiles("prod", "test");
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieName("SPORTAL_XSRF");
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setCookieCustomizer(
                cookie -> cookie.secure(secureCookies).sameSite("Lax").httpOnly(false));

        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId())
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form
                        .loginPage("/acceso")
                        .loginProcessingUrl("/acceso")
                        .successHandler(authenticationSuccessHandler)
                        .failureHandler(authenticationFailureHandler)
                        .permitAll())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/acceso?logout"))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(403)))
                .authorizeHttpRequests(authorize -> {
                    authorize.requestMatchers("/", "/es", "/es/**", "/en", "/en/**", "/public/**",
                            "/public/media/**", "/sitemap.xml", "/robots.txt",
                            "/assets/**", "/error", "/acceso", "/registro", "/preferences/**",
                            "/api/telemetry/errors", "/actuator/health/**",
                            "/verificar-email", "/recuperar", "/restablecer",
                            "/invitacion/aceptar", "/anti-bot/challenge").permitAll();
                    authorize.requestMatchers("/logout").authenticated();
                    AuthorizationRules.routes().forEach(rule ->
                            authorize.requestMatchers(rule.method(), rule.path()).access((authentication, context) -> {
                                var authorities = authentication.get().getAuthorities();
                                boolean hasPermission = authorities.stream()
                                        .anyMatch(authority -> rule.permission().equals(authority.getAuthority()));
                                boolean hasRole = rule.role() == null || authorities.stream()
                                        .anyMatch(authority -> ("ROLE_" + rule.role()).equals(authority.getAuthority()));
                                return new AuthorizationDecision(hasPermission && hasRole);
                            }));
                    authorize.requestMatchers("/cuenta/**").authenticated();
                    authorize.anyRequest().denyAll();
                })
                .headers(headers -> {
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.frameOptions(frame -> frame.deny());
                    headers.referrerPolicy(referrer ->
                            referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.httpStrictTransportSecurity(hsts ->
                            hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000));
                    headers.addHeaderWriter(new StaticHeadersWriter("Permissions-Policy", "microphone=(self)"));
                });
        http.addFilterBefore(cspNonceFilter, HeaderWriterFilter.class);
        http.addFilterBefore(correlationIdFilter, CspNonceFilter.class);
        http.addFilterBefore(rateLimitFilter,
                org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(adminMfaFilter,
                org.springframework.security.web.access.intercept.AuthorizationFilter.class);
        return http.build();
    }
}
