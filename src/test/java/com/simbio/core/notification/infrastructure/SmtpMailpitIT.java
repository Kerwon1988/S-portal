package com.simbio.core.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.application.NotificationProperties;
import com.simbio.core.shared.value.EmailAddress;

class SmtpMailpitIT {

    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("\"ID\"\\s*:\\s*\"([^\"]+)\"");

    @Test
    @EnabledIfSystemProperty(named = "portal.test.mailpit.enabled", matches = "true")
    void deliversLocalizedMultipartEmailToMailpit() throws Exception {
        UUID messageId = UUID.randomUUID();
        String recipient = "mailpit-" + messageId + "@example.test";
        String httpBaseUrl = System.getProperty("portal.test.mailpit.http-base-url", "http://localhost:8026");

        smtpSender().deliver(messageId, recipient, EmailCommand.toAddress(
                EmailAddress.of(recipient),
                EmailTemplate.EMAIL_VERIFICATION,
                Locale.ENGLISH,
                Map.of("url", "https://portal.example.test/verify")));

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        String query = URLEncoder.encode("to:" + recipient, StandardCharsets.UTF_8);
        HttpRequest searchRequest = HttpRequest.newBuilder()
                .uri(URI.create(httpBaseUrl + "/api/v1/search?query=" + query))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();
        String storedMessageId = findStoredMessageId(client, searchRequest, recipient);

        HttpResponse<String> message = client.send(HttpRequest.newBuilder()
                        .uri(URI.create(httpBaseUrl + "/api/v1/message/" + storedMessageId))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertThat(message.statusCode()).isEqualTo(200);
        assertThat(message.body())
                .contains("\"Subject\":\"Verify your email\"")
                .contains("\"HTML\":")
                .contains("\"Text\":")
                .contains("Confirm your email address")
                .contains(recipient);
    }

    private static String findStoredMessageId(
            HttpClient client,
            HttpRequest searchRequest,
            String recipient) throws Exception {
        Instant deadline = Instant.now().plusSeconds(10);
        while (Instant.now().isBefore(deadline)) {
            HttpResponse<String> response = client.send(searchRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isEqualTo(200);
            Matcher matcher = MESSAGE_ID_PATTERN.matcher(response.body());
            if (matcher.find()) {
                return matcher.group(1);
            }
            Thread.sleep(250);
        }
        throw new AssertionError("Mailpit did not store the synthetic verification email for " + recipient);
    }

    private static SmtpEmailSender smtpSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(System.getProperty("portal.test.mailpit.smtp-host", "localhost"));
        mailSender.setPort(Integer.getInteger("portal.test.mailpit.smtp-port", 1026));
        mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        mailSender.getJavaMailProperties().setProperty("mail.smtp.connectiontimeout", "3000");
        mailSender.getJavaMailProperties().setProperty("mail.smtp.timeout", "3000");
        mailSender.getJavaMailProperties().setProperty("mail.smtp.writetimeout", "3000");

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("i18n/notification/messages");
        TemplateEngine htmlTemplateEngine = htmlTemplateEngine(messageSource);
        NotificationProperties properties = new NotificationProperties();
        properties.setFromAddress("no-reply@example.test");
        return new SmtpEmailSender(mailSender, messageSource, htmlTemplateEngine, properties);
    }

    private static TemplateEngine htmlTemplateEngine(ResourceBundleMessageSource messageSource) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolver.setCacheable(false);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setMessageSource(messageSource);
        return engine;
    }
}
