package com.simbio.core.notification.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import com.simbio.core.notification.api.EmailCommand;
import com.simbio.core.notification.api.EmailTemplate;
import com.simbio.core.notification.application.NotificationProperties;
import com.simbio.core.shared.value.EmailAddress;

class SmtpEmailSenderTest {

    @Test
    void rendersLocalizedPlainAndHtmlBodiesWithEscapedActionUrl() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MessageSource messageSource = localizedMessageSource();
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        NotificationProperties properties = new NotificationProperties();
        properties.setFromAddress("no-reply@example.test");
        SmtpEmailSender sender = new SmtpEmailSender(mailSender, messageSource, htmlTemplateEngine(), properties);
        UUID messageId = UUID.randomUUID();
        EmailCommand command = EmailCommand.toAddress(
                EmailAddress.of("synthetic@example.test"),
                EmailTemplate.EMAIL_VERIFICATION,
                Locale.ENGLISH,
                Map.of("url", "https://portal.example.test/verify?token=synthetic&source=email"));

        sender.deliver(messageId, "synthetic@example.test", command);

        assertThat(mimeMessage.getSubject()).isEqualTo("Verify your email");
        assertThat(mimeMessage.getFrom()[0].toString()).contains("no-reply@example.test");
        assertThat(mimeMessage.getRecipients(Message.RecipientType.TO)[0].toString())
                .contains("synthetic@example.test");
        assertThat(mimeMessage.getHeader("Message-ID")[0]).isEqualTo("<" + messageId + "@s-portal>");
        String content = bodyParts(mimeMessage);
        assertThat(content).contains("Confirm your email address");
        assertThat(content).contains("Copy this link:");
        assertThat(content).contains("https://portal.example.test/verify?token=synthetic&amp;source=email");
        assertThat(content).doesNotContain("<script>");
        verify(mailSender).send(same(mimeMessage));
    }

    @Test
    void rendersSpanishEmailCopyWhenRecipientLocaleIsSpanish() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MessageSource messageSource = localizedMessageSource();
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        NotificationProperties properties = new NotificationProperties();
        properties.setFromAddress("no-reply@example.test");
        SmtpEmailSender sender = new SmtpEmailSender(mailSender, messageSource, htmlTemplateEngine(), properties);
        EmailCommand command = EmailCommand.toAddress(
                EmailAddress.of("synthetic@example.test"),
                EmailTemplate.EMAIL_VERIFICATION,
                Locale.forLanguageTag("es"),
                Map.of("url", "https://portal.example.test/verificar?token=synthetic"));

        sender.deliver(UUID.randomUUID(), "synthetic@example.test", command);

        assertThat(mimeMessage.getSubject()).isEqualTo("Verifica tu correo");
        assertThat(bodyParts(mimeMessage)).contains("Confirma tu correo para activar la cuenta.")
                .contains("Copia este enlace:");
        verify(mailSender).send(same(mimeMessage));
    }

    @Test
    void rendersAccountSecurityNotificationWithoutARecoveryLink() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        NotificationProperties properties = new NotificationProperties();
        properties.setFromAddress("no-reply@example.test");
        SmtpEmailSender sender = new SmtpEmailSender(
                mailSender, localizedMessageSource(), htmlTemplateEngine(), properties);
        EmailCommand command = EmailCommand.toAddress(
                EmailAddress.of("synthetic@example.test"),
                EmailTemplate.ACCOUNT_SECURITY_CHANGED,
                Locale.ENGLISH,
                Map.of());

        sender.deliver(UUID.randomUUID(), "synthetic@example.test", command);

        assertThat(mimeMessage.getSubject()).isEqualTo("Your account security settings changed");
        assertThat(bodyParts(mimeMessage))
                .contains("An administrator changed a security setting on your account.")
                .doesNotContain("Review account");
        verify(mailSender).send(same(mimeMessage));
    }

    @Test
    void rejectsNonHttpActionUrlsWithoutSendingMail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MessageSource messageSource = localizedMessageSource();
        NotificationProperties properties = new NotificationProperties();
        properties.setFromAddress("no-reply@example.test");
        SmtpEmailSender sender = new SmtpEmailSender(mailSender, messageSource, htmlTemplateEngine(), properties);
        EmailCommand command = EmailCommand.toAddress(
                EmailAddress.of("synthetic@example.test"),
                EmailTemplate.PASSWORD_RECOVERY,
                Locale.forLanguageTag("es"),
                Map.of("url", "javascript:alert(1)"));

        assertThatThrownBy(() -> sender.deliver(UUID.randomUUID(), "synthetic@example.test", command))
                .isInstanceOf(MailException.class)
                .hasMessage("Could not compose notification email");
        verifyNoInteractions(mailSender);
    }

    private static MessageSource localizedMessageSource() {
        MessageSource messageSource = mock(MessageSource.class);
        when(messageSource.getMessage(anyString(), isNull(), any(Locale.class)))
                .thenAnswer(invocation -> {
                    String code = invocation.getArgument(0);
                    Locale locale = invocation.getArgument(2);
                    String language = locale.getLanguage();
                    return switch (code) {
                        case "notification.email.brand" -> "Simbio";
                        case "notification.email.fallback" -> language.equals("es")
                                ? "Copia este enlace:" : "Copy this link:";
                        case "notification.email.verification.subject" -> language.equals("es")
                                ? "Verifica tu correo" : "Verify your email";
                        case "notification.email.verification.body" -> language.equals("es")
                                ? "Confirma tu correo para activar la cuenta."
                                : "Confirm your email address to activate your account.";
                        case "notification.email.verification.action" -> language.equals("es")
                                ? "Verificar correo" : "Verify email";
                        case "notification.email.recovery.subject" -> language.equals("es")
                                ? "Restablece tu contraseña" : "Reset your password";
                        case "notification.email.recovery.body" -> language.equals("es")
                                ? "Has solicitado restablecer la contraseña."
                                : "You requested a password reset.";
                        case "notification.email.recovery.action" -> language.equals("es")
                                ? "Restablecer contraseña" : "Reset password";
                        case "notification.email.account-security-changed.subject" -> language.equals("es")
                                ? "Han cambiado los ajustes de seguridad de tu cuenta"
                                : "Your account security settings changed";
                        case "notification.email.account-security-changed.body" -> language.equals("es")
                                ? "Un administrador ha cambiado un ajuste de seguridad de tu cuenta."
                                : "An administrator changed a security setting on your account.";
                        case "notification.email.account-security-changed.action" -> language.equals("es")
                                ? "Revisar cuenta" : "Review account";
                        default -> throw new IllegalArgumentException("Unexpected message key");
                    };
                });
        return messageSource;
    }

    private static TemplateEngine htmlTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        TemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static String bodyParts(Part part) throws Exception {
        Object content = part.getContent();
        if (content instanceof String text) {
            return text;
        }
        if (content instanceof Multipart multipart) {
            StringBuilder result = new StringBuilder();
            for (int index = 0; index < multipart.getCount(); index++) {
                result.append(bodyParts(multipart.getBodyPart(index)));
            }
            return result.toString();
        }
        return "";
    }
}
