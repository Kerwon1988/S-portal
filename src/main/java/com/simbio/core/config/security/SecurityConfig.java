package com.simbio.core.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
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
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            Environment environment,
            CorrelationIdFilter correlationIdFilter,
            CspNonceFilter cspNonceFilter)
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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.logoutUrl("/logout"))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> response.sendError(403)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/es", "/es/**", "/en", "/en/**", "/login", "/public/**",
                                "/assets/**", "/error", "/preferences/**", "/api/telemetry/errors").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()
                        .anyRequest().denyAll())
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
        return http.build();
    }
}
