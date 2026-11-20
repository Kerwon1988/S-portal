package com.simbio.core.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.application.NotificationProperties;

class OperationalEmailSenderTest {

    @Test
    void rendersEnglishRequestEmailWithInternalTokenFreePlainAndHtmlLink() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mime);
        NotificationProperties properties = new NotificationProperties();
        properties.setFromAddress("no-reply@example.test");
        SmtpEmailSender sender = new SmtpEmailSender(mailSender, messageSource(), templateEngine(), properties,
                "https://portal.example.test");
        EmailCommand command = EmailCommand.toUser(UUID.randomUUID(), EmailTemplate.REQUEST_STATUS_CHANGED,
                Locale.ENGLISH, Map.of("url", "/cuenta/solicitudes/synthetic"));

        sender.deliver(UUID.randomUUID(), "synthetic@example.test", command);

        assertThat(mime.getSubject()).isEqualTo("Your request was updated");
        String content = bodyParts(mime);
        assertThat(content).contains("Review the details in the secure panel.")
                .contains("https://portal.example.test/cuenta/solicitudes/synthetic")
                .doesNotContain("token=")
                .doesNotContain("client-name");
    }

    private static MessageSource messageSource() {
        MessageSource source = mock(MessageSource.class);
        when(source.getMessage(anyString(), isNull(), any(Locale.class))).thenAnswer(invocation -> switch (
                invocation.getArgument(0, String.class)) {
            case "notification.email.brand" -> "Simbio";
            case "notification.email.fallback" -> "If the button does not work, copy this link:";
            case "notification.email.request-status-changed.subject" -> "Your request was updated";
            case "notification.email.request-status-changed.body" ->
                    "The status of one of your requests changed. Review the details in the secure panel.";
            case "notification.email.request-status-changed.action" -> "View request";
            default -> "Synthetic text";
        });
        return source;
    }

    private static TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static String bodyParts(MimeMessage message) throws Exception {
        return collect(message.getContent());
    }

    private static String collect(Object content) throws Exception {
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder text = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                Part part = multipart.getBodyPart(index);
                text.append(collect(part.getContent()));
            }
            return text.toString();
        }
        return "";
    }
}
