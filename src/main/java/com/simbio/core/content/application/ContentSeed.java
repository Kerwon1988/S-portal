package com.simbio.core.content.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.simbio.core.content.domain.ApprovalState;
import com.simbio.core.content.domain.ContentBlockEntity;
import com.simbio.core.content.domain.ContentBlockType;
import com.simbio.core.content.domain.ContentPageType;
import com.simbio.core.content.domain.PageEntity;
import com.simbio.core.content.domain.PageTranslationEntity;
import com.simbio.core.content.domain.PageVersionEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ContentSeed implements ApplicationRunner {

    private static final UUID SEED_ACTOR = new UUID(0, 0);

    private final ContentStore store;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    ContentSeed(ContentStore store, ObjectMapper objectMapper) {
        this(store, objectMapper, Clock.systemUTC());
    }

    ContentSeed(ContentStore store, ObjectMapper objectMapper, Clock clock) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedHome("es", "inicio", "Menos gestión, más negocio.",
                "Una base digital para ayudar a tu negocio a trabajar con más claridad.",
                "Accede al portal");
        seedHome("en", "home", "Less admin, more business.",
                "A digital foundation to help your business work with greater clarity.",
                "Access the portal");
        seedDraftPage("simbiosphere", ContentPageType.PAGE, "es", "simbiosphere",
                "SimbioSphere", "Contenido de producto pendiente de revisión.");
        seedDraftPage("simbiosphere", ContentPageType.PAGE, "en", "simbiosphere",
                "SimbioSphere", "Product information is pending review.");
        seedDraftPage("services-development", ContentPageType.PAGE, "es", "servicios/desarrollo",
                "Desarrollo a medida", "Contenido de servicios pendiente de revisión.");
        seedDraftPage("services-development", ContentPageType.PAGE, "en", "services/development",
                "Custom development", "Service information is pending review.");
        seedAbout("es", "sobre-nosotros", "Sobre nosotros",
                "La información de empresa y equipo se mostrará cuando esté aprobada.");
        seedAbout("en", "about-us", "About us",
                "Company and team information will appear when it has been approved.");
        seedDraftPage("request-conditions", ContentPageType.REQUEST_CONDITIONS, "es",
                "condiciones-solicitud", "Condiciones de solicitud",
                "Las condiciones están pendientes de aprobación comercial y legal.");
        seedDraftPage("request-conditions", ContentPageType.REQUEST_CONDITIONS, "en",
                "request-conditions", "Request conditions",
                "Conditions are pending commercial and legal approval.");
        for (String type : List.of("NOTICE", "PRIVACY", "COOKIES", "TERMS", "MARKETING")) {
            if (type.equals("MARKETING")) {
                seedLegalDraft(type, "es", "Quiero recibir comunicaciones comerciales de Simbio sobre sus productos "
                        + "y servicios. Puedo retirar mi consentimiento en cualquier momento.");
                seedLegalDraft(type, "en", "I agree to receive marketing communications from Simbio about its products "
                        + "and services. I can withdraw my consent at any time.");
            } else {
                seedLegalDraft(type, "es", "Documento pendiente de aprobación legal.");
                seedLegalDraft(type, "en", "Document pending legal approval.");
            }
        }
    }

    private void seedHome(String locale, String slug, String title, String intro, String callToAction) {
        PageEntity page = store.findPageByKey("home").orElseGet(() ->
                new PageEntity(UUID.randomUUID(), "home", ContentPageType.PAGE, Instant.now(clock)));
        PageTranslationEntity translation = page.translation(locale);
        if (translation.getPublishedSnapshotJson() != null) {
            return;
        }
        translation.edit(slug, title, title, intro, ApprovalState.APPROVED, SEED_ACTOR, Instant.now(clock));
        translation.replaceBlocks(List.of(
                new ContentBlockEntity(ContentBlockType.HERO, 0, title, intro, null, null),
                new ContentBlockEntity(ContentBlockType.CARD, 1, "SimbioSphere",
                        locale.equals("es") ? "La información de producto se publicará tras su verificación."
                                : "Product information will be published after verification.",
                        null, null),
                new ContentBlockEntity(ContentBlockType.CARD, 2,
                        locale.equals("es") ? "Desarrollo a medida" : "Custom development",
                        locale.equals("es") ? "Los detalles del servicio están pendientes de publicación."
                                : "Service details are pending publication.",
                        null, null),
                new ContentBlockEntity(ContentBlockType.TEXT, 3,
                        locale.equals("es") ? "Automatización futura" : "Future automation",
                        locale.equals("es")
                                ? "La automatización de tareas sencillas se explorará como línea futura, sin comprometer disponibilidad."
                                : "Automation of simple tasks is a future direction to explore, with no availability commitment.",
                        null, null),
                new ContentBlockEntity(ContentBlockType.CTA, 4, callToAction, "", "/acceso", null)));
        PageDraftView draft = new PageDraftView(page.getPublicId(), translation.getPublicId(), "home",
                ContentPageType.PAGE, locale, slug, title, title, intro,
                com.simbio.core.content.domain.TranslationState.DRAFT, translation.getVersion(),
                List.of(new BlockInput(ContentBlockType.HERO.name(), 0, title, intro, null, null),
                new BlockInput(ContentBlockType.CARD.name(), 1, "SimbioSphere",
                                locale.equals("es") ? "La información de producto se publicará tras su verificación."
                                        : "Product information will be published after verification.", null, null),
                        new BlockInput(ContentBlockType.CARD.name(), 2,
                                locale.equals("es") ? "Desarrollo a medida" : "Custom development",
                                locale.equals("es") ? "Los detalles del servicio están pendientes de publicación."
                                        : "Service details are pending publication.", null, null),
                        new BlockInput(ContentBlockType.TEXT.name(), 3,
                                locale.equals("es") ? "Automatización futura" : "Future automation",
                                locale.equals("es")
                                        ? "La automatización de tareas sencillas se explorará como línea futura, sin comprometer disponibilidad."
                                        : "Automation of simple tasks is a future direction to explore, with no availability commitment.",
                                null, null),
                        new BlockInput(ContentBlockType.CTA.name(), 4, callToAction, "", "/acceso", null)));
        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(ContentSnapshot.from(draft));
        } catch (JacksonException exception) {
            throw new IllegalStateException("Could not serialize initial CMS content", exception);
        }
        Instant publishedAt = Instant.now(clock);
        translation.publish(snapshotJson, SEED_ACTOR, publishedAt);
        store.savePage(page);
        store.saveVersion(new PageVersionEntity(translation.getPublicId(), translation.getVersion(),
                snapshotJson, SEED_ACTOR, publishedAt));
    }

    private void seedDraftPage(String pageKey, ContentPageType type, String locale, String slug,
            String title, String text) {
        PageEntity page = store.findPageByKey(pageKey).orElseGet(() ->
                new PageEntity(UUID.randomUUID(), pageKey, type, Instant.now(clock)));
        PageTranslationEntity translation = page.translation(locale);
        if (translation.getUpdatedAt().isAfter(Instant.EPOCH)) {
            return;
        }
        translation.edit(slug, title, title, text, ApprovalState.PENDING, SEED_ACTOR, Instant.now(clock));
        translation.replaceBlocks(List.of(new ContentBlockEntity(
                ContentBlockType.TEXT, 0, title, text, null, null)));
        store.savePage(page);
    }

    private void seedLegalDraft(String type, String locale, String pendingText) {
        if (store.findLatestLegalDocument(type, locale).isPresent()) {
            return;
        }
        String title = type + " — pendiente de aprobación";
        store.saveLegalDocument(new com.simbio.core.content.domain.LegalDocumentVersionEntity(
                UUID.randomUUID(), type, locale, 1, title, pendingText, "PENDING", null,
                SEED_ACTOR, Instant.now(clock)));
    }

    private void seedAbout(String locale, String slug, String title, String body) {
        PageEntity page = store.findPageByKey("about-us").orElseGet(() ->
                new PageEntity(UUID.randomUUID(), "about-us", ContentPageType.PAGE, Instant.now(clock)));
        PageTranslationEntity translation = page.translation(locale);
        if (translation.getPublishedSnapshotJson() != null) {
            return;
        }
        translation.edit(slug, title, title, body, ApprovalState.PENDING, SEED_ACTOR, Instant.now(clock));
        translation.replaceBlocks(List.of(new ContentBlockEntity(ContentBlockType.TEXT, 0, title, body, null, null)));
        store.savePage(page);
    }
}
