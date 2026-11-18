package com.simbio.core.notification.infrastructure;

import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.application.EmailDeliveryPort;
import com.simbio.core.notification.application.NotificationProperties;

@Component
public class SmtpEmailSender implements EmailDeliveryPort {

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;
    private final TemplateEngine htmlTemplateEngine;
    private final TemplateEngine plainTextTemplateEngine;
    private final NotificationProperties properties;

    SmtpEmailSender(
            JavaMailSender mailSender,
            MessageSource messageSource,
            TemplateEngine htmlTemplateEngine,
            NotificationProperties properties) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
        this.htmlTemplateEngine = htmlTemplateEngine;
        this.properties = properties;
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.TEXT);
        plainTextTemplateEngine = new SpringTemplateEngine();
        plainTextTemplateEngine.setTemplateResolver(resolver);
    }

    @Override
    public void deliver(UUID messageId, String recipient, EmailCommand command) throws MailException {
        Locale locale = Locale.forLanguageTag(command.locale().getLanguage());
        String key = messageKey(command.template());
        Context context = new Context(locale);
        context.setVariables(Map.of(
                "subject", messageSource.getMessage(key + ".subject", null, locale),
                "body", messageSource.getMessage(key + ".body", null, locale),
                "actionLabel", messageSource.getMessage(key + ".action", null, locale),
                "fallback", messageSource.getMessage("notification.email.fallback", null, locale),
                "actionUrl", actionUrl(command.variables().getOrDefault("url", "")),
                "brand", messageSource.getMessage("notification.email.brand", null, locale)));
        String html = htmlTemplateEngine.process("notification/email/message", context);
        String plain = plainTextTemplateEngine.process("""
                [[${brand}]]

                [[${body}]]
                [[${actionUrl}]]
                """, context);
        String subject = String.valueOf(context.getVariable("subject"));

        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(requiredFromAddress());
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(plain, html);
            message.setHeader("Message-ID", "<" + messageId + "@s-portal>");
            mailSender.send(message);
        } catch (MessagingException exception) {
            throw new MailCompositionException(exception);
        }
    }

    private String requiredFromAddress() {
        if (properties.getFromAddress() == null || properties.getFromAddress().isBlank()) {
            throw new IllegalStateException("Notification sender address is not configured");
        }
        return properties.getFromAddress();
    }

    private static String actionUrl(String value) {
        if (value.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(value);
            String scheme = uri.getScheme();
            if (!uri.isAbsolute() || uri.getHost() == null || uri.getUserInfo() != null
                    || !("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("Notification action URL must use HTTP or HTTPS");
            }
            return uri.toASCIIString();
        } catch (IllegalArgumentException exception) {
            throw new MailCompositionException(exception);
        }
    }

    private static String messageKey(EmailTemplate template) {
        return "notification.email." + switch (template) {
            case EMAIL_VERIFICATION -> "verification";
            case PASSWORD_RECOVERY -> "recovery";
            case PASSWORD_CHANGED -> "password-changed";
            case ACCOUNT_SECURITY_CHANGED -> "account-security-changed";
            case INVITATION -> "invitation";
        };
    }

    private static final class MailCompositionException extends MailException {
        private static final long serialVersionUID = 1L;

        private MailCompositionException(Exception cause) {
            super("Could not compose notification email", cause);
        }
    }
}
