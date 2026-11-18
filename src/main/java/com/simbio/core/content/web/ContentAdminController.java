package com.simbio.core.content.web;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.simbio.core.content.application.ContentApprovalRequiredException;
import com.simbio.core.content.application.ContentMediaNotCleanException;
import com.simbio.core.content.application.ContentService;
import com.simbio.core.content.application.ContentValidationException;
import com.simbio.core.content.application.LegalDocumentService;
import com.simbio.core.content.application.PageDraftView;
import com.simbio.core.content.application.PublishedPageView;
import com.simbio.core.shared.spi.CurrentUser;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestion/contenido")
public class ContentAdminController {

    private static final int PAGE_SIZE = 20;

    private final ContentService contentService;
    private final LegalDocumentService legalDocumentService;
    private final CurrentUser currentUser;

    public ContentAdminController(ContentService contentService, LegalDocumentService legalDocumentService,
            CurrentUser currentUser) {
        this.contentService = contentService;
        this.legalDocumentService = legalDocumentService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(@RequestParam(name = "missingLocale", required = false) String missingLocale,
            @RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        if (page < 0) {
            throw new ContentValidationException("Page number cannot be negative");
        }
        List<PageDraftView> allPages = contentService.listForEditor(user());
        Map<String, List<PageDraftView>> byPage = allPages.stream()
                .collect(Collectors.groupingBy(PageDraftView::pageKey));
        String normalizedMissingLocale = normalizeMissingLocale(missingLocale);
        List<PageDraftView> filteredPages = normalizedMissingLocale == null ? allPages : allPages.stream()
                .filter(draft -> byPage.get(draft.pageKey()).stream()
                        .noneMatch(translation -> translation.locale().equals(normalizedMissingLocale)
                                && translation.isPublished()))
                .toList();
        int pageCount = (int) Math.ceil((double) filteredPages.size() / PAGE_SIZE);
        if (page > 0 && (pageCount == 0 || page >= pageCount)) {
            throw new ContentValidationException("Page number is outside the result range");
        }
        int fromIndex = Math.min(page * PAGE_SIZE, filteredPages.size());
        int toIndex = Math.min(fromIndex + PAGE_SIZE, filteredPages.size());
        model.addAttribute("pages", filteredPages.subList(fromIndex, toIndex));
        model.addAttribute("missingLocale", normalizedMissingLocale);
        model.addAttribute("translationPending", byPage.entrySet().stream()
                .filter(entry -> !hasPublishedTranslation(entry.getValue(), "es")
                        || !hasPublishedTranslation(entry.getValue(), "en"))
                .map(Map.Entry::getKey).collect(Collectors.toSet()));
        model.addAttribute("pageNumber", page);
        model.addAttribute("pageCount", pageCount);
        model.addAttribute("hasPrevious", page > 0);
        model.addAttribute("hasNext", page + 1 < pageCount);
        return "content/admin/list";
    }

    @GetMapping("/nuevo")
    public String newPage(Model model) {
        model.addAttribute("form", new ContentPageForm());
        model.addAttribute("media", contentService.mediaLibrary(user()));
        return "content/admin/editor";
    }

    @GetMapping("/{pagePublicId}")
    public String edit(@PathVariable UUID pagePublicId, @RequestParam(defaultValue = "es") String locale,
            Model model) {
        model.addAttribute("form", ContentPageForm.from(contentService.draft(user(), pagePublicId, locale)));
        model.addAttribute("media", contentService.mediaLibrary(user()));
        return "content/admin/editor";
    }

    @PostMapping("/crear")
    public String create(@ModelAttribute("form") ContentPageForm form, RedirectAttributes redirectAttributes) {
        PageDraftView saved = contentService.saveDraft(user(), form.toCommand());
        redirectAttributes.addAttribute("locale", saved.locale());
        return "redirect:/gestion/contenido/" + saved.pagePublicId();
    }

    @PostMapping("/{pagePublicId}/guardar")
    public String save(@PathVariable UUID pagePublicId, @ModelAttribute("form") ContentPageForm form,
            RedirectAttributes redirectAttributes) {
        form.setPagePublicId(pagePublicId);
        PageDraftView saved = contentService.saveDraft(user(), form.toCommand());
        redirectAttributes.addAttribute("locale", saved.locale());
        return "redirect:/gestion/contenido/" + pagePublicId;
    }

    @GetMapping("/{pagePublicId}/preview")
    public String preview(@PathVariable UUID pagePublicId, @RequestParam(defaultValue = "es") String locale,
            Model model, HttpServletResponse response) {
        PublishedPageView page = contentService.preview(user(), pagePublicId, locale);
        model.addAttribute("page", page);
        model.addAttribute("seoTitle", page.seoTitle());
        model.addAttribute("metaDescription", page.metaDescription());
        model.addAttribute("canonicalPath", page.canonicalPath());
        model.addAttribute("alternatePaths", page.alternatePaths());
        model.addAttribute("contentLocale", page.locale());
        model.addAttribute("isPreview", true);
        response.setHeader("Cache-Control", "no-store, max-age=0");
        response.setHeader("X-Robots-Tag", "noindex, nofollow");
        return "content/page";
    }

    @PostMapping("/{pagePublicId}/publicar")
    public String publish(@PathVariable UUID pagePublicId, @RequestParam String locale,
            RedirectAttributes redirectAttributes) {
        try {
            contentService.publish(user(), pagePublicId, locale);
            redirectAttributes.addFlashAttribute("noticeKey", "content.notice.published");
        } catch (ContentApprovalRequiredException exception) {
            redirectAttributes.addFlashAttribute("errorKey", "content.error.approval");
        }
        redirectAttributes.addAttribute("locale", locale);
        return "redirect:/gestion/contenido/" + pagePublicId;
    }

    @GetMapping("/{pagePublicId}/versiones")
    public String versions(@PathVariable UUID pagePublicId, @RequestParam UUID translation,
            Model model) {
        model.addAttribute("pagePublicId", pagePublicId);
        model.addAttribute("versions", contentService.versions(user(), translation));
        return "content/admin/versions";
    }

    @PostMapping("/{pagePublicId}/restaurar")
    public String restore(@PathVariable UUID pagePublicId, @RequestParam String locale,
            RedirectAttributes redirectAttributes) {
        contentService.restorePrevious(user(), pagePublicId, locale);
        redirectAttributes.addAttribute("locale", locale);
        return "redirect:/gestion/contenido/" + pagePublicId;
    }

    @GetMapping("/medios")
    public String media(Model model) {
        model.addAttribute("media", contentService.mediaLibrary(user()));
        return "content/admin/media";
    }

    @GetMapping("/medios/{publicId}/preview")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> previewMedia(
            @PathVariable UUID publicId) {
        return mediaResponse(contentService.openPreviewMedia(user(), publicId));
    }

    @GetMapping("/medios/{publicId}/preview/variants/{variantCode}")
    @org.springframework.web.bind.annotation.ResponseBody
    public org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> previewMediaVariant(
            @PathVariable UUID publicId, @PathVariable String variantCode) {
        return mediaResponse(contentService.openPreviewImageVariant(user(), publicId, variantCode));
    }

    @GetMapping("/legal")
    public String legalDocuments(Model model) {
        model.addAttribute("documents", legalDocumentService.drafts(user()));
        return "content/admin/legal";
    }

    @PostMapping("/legal")
    public String saveLegalDraft(@RequestParam String documentType, @RequestParam String locale,
            @RequestParam String title, @RequestParam String contentMarkdown,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveDate,
            RedirectAttributes redirectAttributes) {
        legalDocumentService.saveDraft(user(), documentType, locale, title, contentMarkdown, effectiveDate);
        redirectAttributes.addFlashAttribute("noticeKey", "content.legal.saved");
        return "redirect:/gestion/contenido/legal";
    }

    @PostMapping("/medios")
    public String upload(@RequestParam("file") MultipartFile file, @RequestParam String altEs,
            @RequestParam String altEn, RedirectAttributes redirectAttributes) throws IOException {
        try {
            contentService.storeMedia(user(), file.getInputStream(), file.getOriginalFilename(),
                    file.getSize(), altEs, altEn);
            redirectAttributes.addFlashAttribute("noticeKey", "content.media.uploaded");
        } catch (ContentMediaNotCleanException exception) {
            redirectAttributes.addFlashAttribute("errorKey", "content.error.media-scan");
        } catch (ContentValidationException exception) {
            redirectAttributes.addFlashAttribute("errorKey", "content.error.validation");
        }
        return "redirect:/gestion/contenido/medios";
    }

    private com.simbio.core.shared.security.AuthenticatedUser user() {
        return currentUser.currentUser().orElseThrow(com.simbio.core.content.application.ContentPermissionException::new);
    }

    private static String normalizeMissingLocale(String locale) {
        if (locale == null || locale.isBlank()) {
            return null;
        }
        if (!locale.equals("es") && !locale.equals("en")) {
            throw new ContentValidationException("Only es and en locales are supported");
        }
        return locale;
    }

    private static boolean hasPublishedTranslation(List<PageDraftView> translations, String locale) {
        return translations.stream().anyMatch(translation -> translation.locale().equals(locale)
                && translation.isPublished());
    }

    private static org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> mediaResponse(
            com.simbio.core.content.application.PublicMedia media) {
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(media.detectedMime()))
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .body(new org.springframework.core.io.InputStreamResource(media.content()));
    }
}
