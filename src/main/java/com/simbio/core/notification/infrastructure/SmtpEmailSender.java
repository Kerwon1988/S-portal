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
    private final URI baseUri;

    @org.springframework.beans.factory.annotation.Autowired
    SmtpEmailSender(
            JavaMailSender mailSender,
            MessageSource messageSource,
            TemplateEngine htmlTemplateEngine,
            NotificationProperties properties,
            @org.springframework.beans.factory.annotation.Value("${portal.base-url}") String baseUrl) {
        this(mailSender, messageSource, htmlTemplateEngine, properties, java.util.Objects.requireNonNull(
                URI.create(baseUrl)));
    }

    SmtpEmailSender(
            JavaMailSender mailSender,
            MessageSource messageSource,
            TemplateEngine htmlTemplateEngine,
            NotificationProperties properties) {
        this(mailSender, messageSource, htmlTemplateEngine, properties, (URI) null);
    }

    private SmtpEmailSender(
            JavaMailSender mailSender,
            MessageSource messageSource,
            TemplateEngine htmlTemplateEngine,
            NotificationProperties properties,
            URI baseUri) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
        this.htmlTemplateEngine = htmlTemplateEngine;
        this.properties = properties;
        this.baseUri = baseUri;
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
                "actionUrl", actionUrl(command.variables().getOrDefault("url",
                        command.variables().getOrDefault("actionUrl", "")), command.isOperational(), baseUri),
                "brand", messageSource.getMessage("notification.email.brand", null, locale)));
        String html = htmlTemplateEngine.process(templatePath(command.template()), context);
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

    private static String actionUrl(String value, boolean operational, URI baseUri) {
        if (value.isBlank()) {
            return "";
        }
        try {
            URI uri = URI.create(value);
            if (operational && baseUri != null) {
                uri = baseUri.resolve(uri);
                if (!sameOrigin(baseUri, uri)) {
                    throw new IllegalArgumentException("Operational email URL must remain on the portal origin");
                }
            }
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

    private static boolean sameOrigin(URI left, URI right) {
        return left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private static int effectivePort(URI uri) {
        return uri.getPort() >= 0 ? uri.getPort() : "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private static String messageKey(EmailTemplate template) {
        return "notification.email." + switch (template) {
            case EMAIL_VERIFICATION -> "verification";
            case PASSWORD_RECOVERY -> "recovery";
            case PASSWORD_CHANGED -> "password-changed";
            case ACCOUNT_SECURITY_CHANGED -> "account-security-changed";
            case INVITATION -> "invitation";
            case REQUEST_CREATED -> "request-created";
            case REQUEST_STATUS_CHANGED -> "request-status-changed";
            case QUOTE_SENT -> "quote-sent";
            case MESSAGE_POSTED -> "message-posted";
            case CONTACT_ACKNOWLEDGEMENT -> "contact-acknowledgement";
            case PROVISIONING_ACTION_REQUIRED -> "provisioning-action-required";
            case PROVISIONING_FAILED -> "provisioning-failed";
        };
    }

    private static String templatePath(EmailTemplate template) {
        return "notification/email/" + switch (template) {
            case REQUEST_CREATED, REQUEST_STATUS_CHANGED, QUOTE_SENT -> "request_status_changed";
            case MESSAGE_POSTED -> "message_posted";
            case PROVISIONING_ACTION_REQUIRED, PROVISIONING_FAILED -> "provisioning_alert";
            default -> "message";
        };
    }

    private static final class MailCompositionException extends MailException {
        private static final long serialVersionUID = 1L;

        private MailCompositionException(Exception cause) {
            super("Could not compose notification email", cause);
        }
    }
}
