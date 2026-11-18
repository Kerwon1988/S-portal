package com.simbio.core.customer.web;

import java.util.Locale;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.simbio.core.customer.application.CustomerRegistrationOutcome;
import com.simbio.core.customer.application.CustomerRegistrationService;
import com.simbio.core.customer.application.InvalidCustomerRegistrationException;
import com.simbio.core.legal.api.ConsentDocumentsUnavailableException;
import com.simbio.core.legal.api.ConsentRecorder;
import com.simbio.core.legal.api.StaleConsentVersionException;
import com.simbio.core.shared.spi.AntiBotVerification;

@Controller
public class CustomerRegistrationController {

    private final CustomerRegistrationService registrations;
    private final ConsentRecorder consentRecorder;
    private final AntiBotVerification antiBot;

    public CustomerRegistrationController(
            CustomerRegistrationService registrations,
            ConsentRecorder consentRecorder,
            AntiBotVerification antiBot) {
        this.registrations = registrations;
        this.consentRecorder = consentRecorder;
        this.antiBot = antiBot;
    }

    @GetMapping("/registro")
    public String form(
            Locale requestLocale,
            @RequestParam(defaultValue = "false") boolean legalUpdated,
            Model model) {
        return prepareForm(model, language(requestLocale), legalUpdated);
    }

    @PostMapping("/registro")
    public String register(
            @Valid @ModelAttribute("registrationForm") CustomerRegistrationForm form,
            BindingResult errors,
            Locale requestLocale,
            Model model) {
        String locale = language(requestLocale);
        if (errors.hasErrors()) {
            return prepareForm(model, locale, false);
        }
        try {
            CustomerRegistrationOutcome outcome = registrations.register(form.toCommand(locale));
            if (outcome == CustomerRegistrationOutcome.REJECTED) {
                errors.reject("customer.registration.antibot.failed");
                return prepareForm(model, locale, false);
            }
            model.addAttribute("submitted", true);
            return "customer/registration-result";
        } catch (InvalidCustomerRegistrationException exception) {
            errors.rejectValue(exception.field(), "customer.registration.invalid");
            return prepareForm(model, locale, false);
        } catch (StaleConsentVersionException exception) {
            return "redirect:/registro?legalUpdated=true";
        } catch (ConsentDocumentsUnavailableException exception) {
            return prepareForm(model, locale, false);
        }
    }

    private String prepareForm(Model model, String locale, boolean legalUpdated) {
        try {
            var versions = consentRecorder.currentDocumentVersions(locale);
            model.addAttribute("legalVersions", versions);
            if (!model.containsAttribute("registrationForm")) {
                model.addAttribute("registrationForm", CustomerRegistrationForm.empty(versions));
            }
            model.addAttribute("registrationAvailable", true);
        } catch (ConsentDocumentsUnavailableException exception) {
            if (!model.containsAttribute("registrationForm")) {
                model.addAttribute("registrationForm", CustomerRegistrationForm.empty(null));
            }
            model.addAttribute("registrationAvailable", false);
        }
        model.addAttribute("formLocale", locale);
        model.addAttribute("legalUpdated", legalUpdated);
        model.addAttribute("challenge", antiBot.issueChallenge());
        return "customer/registration";
    }

    private static String language(Locale locale) {
        return locale != null && "en".equals(locale.getLanguage()) ? "en" : "es";
    }
}
