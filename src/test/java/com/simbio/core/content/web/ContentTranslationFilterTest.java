package com.simbio.core.content.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import com.simbio.core.content.application.BlockInput;
import com.simbio.core.content.application.ContentService;
import com.simbio.core.content.application.ContentValidationException;
import com.simbio.core.content.application.LegalDocumentService;
import com.simbio.core.content.application.PageDraftView;
import com.simbio.core.content.domain.ContentPageType;
import com.simbio.core.content.domain.TranslationState;
import com.simbio.core.shared.security.AuthenticatedUser;
import com.simbio.core.shared.security.Permission;
import com.simbio.core.shared.security.Role;
import com.simbio.core.shared.spi.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

class ContentTranslationFilterTest {

    private final ContentService contentService = mock(ContentService.class);
    private final LegalDocumentService legalDocumentService = mock(LegalDocumentService.class);
    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final AuthenticatedUser editor = new AuthenticatedUser(
            UUID.randomUUID(), Role.ADMIN, Set.of(Permission.CONTENT_EDIT));
    private ContentAdminController controller;
    private List<PageDraftView> translations;

    @BeforeEach
    void setUp() {
        controller = new ContentAdminController(contentService, legalDocumentService, currentUser);
        when(currentUser.currentUser()).thenReturn(Optional.of(editor));
        translations = List.of(
                page("landing", "es", TranslationState.PUBLISHED),
                page("landing", "en", TranslationState.DRAFT),
                page("services", "es", TranslationState.PUBLISHED),
                page("services", "en", TranslationState.PUBLISHED));
        when(contentService.listForEditor(editor)).thenReturn(translations);
    }

    @Test
    @DisplayName("TC-CMS-011: pending locale filter includes pages without a published translation")
    void filtersByMissingPublishedTranslationAndMarksIncompletePages() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.list("en", 0, model)).isEqualTo("content/admin/list");
        assertThat(model.asMap().get("pages")).isEqualTo(List.of(translations.get(0), translations.get(1)));
        assertThat(model.asMap().get("translationPending")).isEqualTo(Set.of("landing"));
        assertThat(model.asMap().get("missingLocale")).isEqualTo("en");
    }

    @Test
    @DisplayName("TC-CMS-005: the editor list is paginated on the server")
    void paginatesTheFilteredEditorList() {
        List<PageDraftView> manyPages = IntStream.range(0, 21)
                .mapToObj(index -> page("page-" + index, "es", TranslationState.PUBLISHED))
                .toList();
        when(contentService.listForEditor(editor)).thenReturn(manyPages);
        ExtendedModelMap firstPage = new ExtendedModelMap();
        ExtendedModelMap lastPage = new ExtendedModelMap();

        controller.list(null, 0, firstPage);
        controller.list(null, 1, lastPage);

        assertThat((List<?>) firstPage.asMap().get("pages")).hasSize(20);
        assertThat(firstPage.asMap().get("pageCount")).isEqualTo(2);
        assertThat(firstPage.asMap().get("hasNext")).isEqualTo(true);
        assertThat((List<?>) lastPage.asMap().get("pages")).hasSize(1);
        assertThat(lastPage.asMap().get("hasPrevious")).isEqualTo(true);
    }

    @Test
    @DisplayName("TC-CMS-011: invalid locale filters are rejected")
    void rejectsUnsupportedLocaleFilter() {
        assertThatThrownBy(() -> controller.list("fr", 0, new ExtendedModelMap()))
                .isInstanceOf(ContentValidationException.class);
    }

    private static PageDraftView page(String key, String locale, TranslationState state) {
        return new PageDraftView(UUID.randomUUID(), UUID.randomUUID(), key, ContentPageType.PAGE, locale,
                key, key, key, "", state, 1, List.<BlockInput>of());
    }
}
