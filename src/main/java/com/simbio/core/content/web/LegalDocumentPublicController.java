package com.simbio.core.content.web;

import com.simbio.core.content.application.ContentNotFoundException;
import com.simbio.core.content.application.LegalDocumentService;
import com.simbio.core.content.api.PublishedLegalDocument;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LegalDocumentPublicController {

    private final LegalDocumentService legalDocuments;

    public LegalDocumentPublicController(LegalDocumentService legalDocuments) {
        this.legalDocuments = legalDocuments;
    }

    @GetMapping("/{lang:es|en}/legal/{slug}")
    public String current(@PathVariable String lang, @PathVariable String slug, Model model) {
        PublishedLegalDocument document = legalDocuments.current(LegalDocumentService.typeForSlug(slug), lang)
                .orElseThrow(ContentNotFoundException::new);
        model.addAttribute("document", document);
        model.addAttribute("seoTitle", document.title());
        model.addAttribute("metaDescription", document.title());
        model.addAttribute("canonicalPath", "/" + lang + "/legal/" + slug);
        model.addAttribute("contentLocale", lang);
        return "content/legal";
    }
}
