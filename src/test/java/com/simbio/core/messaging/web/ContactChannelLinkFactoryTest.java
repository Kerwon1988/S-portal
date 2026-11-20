package com.simbio.core.messaging.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.simbio.core.messaging.config.MessagingChannelProperties;

@SpringBootTest(properties = {
        "portal.messaging.channels.whatsapp-e164-number=+34123456789",
        "portal.messaging.channels.whatsapp-text-es=Hola, necesito ayuda & más",
        "portal.messaging.channels.whatsapp-text-en=Hello, need help & more",
        "portal.messaging.channels.contact-email=contact@simbio.test"
})
@ActiveProfiles("test")
class ContactChannelLinkFactoryTest {

    @Autowired
    private ContactChannelLinkFactory linkFactory;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    void createsEncodedWhatsappAndMailtoUrisFromConfiguration() {
        ContactChannelLinks links = linkFactory.forLocale(Locale.forLanguageTag("es"));

        assertThat(URI.create(links.whatsappHref()).toASCIIString())
                .isEqualTo("https://wa.me/34123456789?text=Hola%2C%20necesito%20ayuda%20%26%20m%C3%A1s");
        assertThat(URI.create(links.mailtoHref()).getScheme()).isEqualTo("mailto");
        assertThat(links.mailtoHref()).isEqualTo("mailto:contact@simbio.test");
    }

    @Test
    void rendersLocalizedAccessibleLinksOnlyForConfiguredChannels() {
        String spanish = renderLinks(linkFactory.forLocale(Locale.forLanguageTag("es")), Locale.forLanguageTag("es"));
        String english = renderLinks(linkFactory.forLocale(Locale.ENGLISH), Locale.ENGLISH);

        assertThat(spanish)
                .contains("Canales de contacto", "WhatsApp", "Correo electrónico",
                        "Contactar por WhatsApp; se abrirá la aplicación o web externa",
                        "Contactar por correo electrónico; se abrirá su aplicación de correo",
                        "min-h-11", "focus-visible:outline-2");
        assertThat(english)
                .contains("Contact channels", "WhatsApp", "Email",
                        "Contact through WhatsApp; an external app or website will open",
                        "Contact by email; your email application will open")
                .contains("mailto:contact@simbio.test")
                .doesNotContain("target=\"_blank\"");
    }

    @Test
    void hidesMissingOrInvalidChannelsAndDoesNotAllowMailtoQueryInjection() {
        ContactChannelLinkFactory missingChannels = new ContactChannelLinkFactory(
                new MessagingChannelProperties("", "", "", ""));
        ContactChannelLinks missingLinks = missingChannels.forLocale(Locale.ENGLISH);

        assertThat(missingLinks.whatsappHref()).isNull();
        assertThat(missingLinks.mailtoHref()).isNull();
        assertThat(renderLinks(missingLinks, Locale.ENGLISH)).doesNotContain("<nav");

        ContactChannelLinkFactory invalidChannels = new ContactChannelLinkFactory(
                new MessagingChannelProperties("javascript:alert(1)", "text", "text", "not-an-email"));
        ContactChannelLinks invalidLinks = invalidChannels.forLocale(Locale.ENGLISH);
        assertThat(invalidLinks.whatsappHref()).isNull();
        assertThat(invalidLinks.mailtoHref()).isNull();

        ContactChannelLinkFactory encodedEmail = new ContactChannelLinkFactory(
                new MessagingChannelProperties("", "", "", "help@example.test?subject=javascript:alert(1)"));
        URI mailto = URI.create(encodedEmail.forLocale(Locale.ENGLISH).mailtoHref());
        assertThat(mailto.getScheme()).isEqualTo("mailto");
        assertThat(mailto.getRawQuery()).isNull();
        assertThat(mailto.getRawSchemeSpecificPart()).contains("%3F");
    }

    private String renderLinks(ContactChannelLinks links, Locale locale) {
        Context context = new Context(locale);
        context.setVariable("channelLinks", links);
        return templateEngine.process("messaging/channel-links", context);
    }
}
