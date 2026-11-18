package com.simbio.core.content.web;

import java.util.UUID;

import com.simbio.core.content.application.TeamMemberService;
import com.simbio.core.shared.spi.CurrentUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/gestion/contenido/equipo")
public class ContentTeamController {

    private final TeamMemberService teamMemberService;
    private final CurrentUser currentUser;

    public ContentTeamController(TeamMemberService teamMemberService, CurrentUser currentUser) {
        this.teamMemberService = teamMemberService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("members", teamMemberService.listForEditor(user()));
        return "content/admin/team";
    }

    @PostMapping
    public String save(@RequestParam(required = false) UUID publicId, @RequestParam String locale,
            @RequestParam int position, @RequestParam String displayName, @RequestParam String roleTitle,
            @RequestParam String bioMarkdown,                         @RequestParam(required = false) UUID photoPublicId,
            @RequestParam(defaultValue = "") String photoAlt, @RequestParam(defaultValue = "") String profileUrl,
            RedirectAttributes redirectAttributes) {
        teamMemberService.save(user(), publicId, locale, position, displayName, roleTitle, bioMarkdown,
                photoPublicId, photoAlt, profileUrl);
        redirectAttributes.addFlashAttribute("noticeKey", "content.team.saved");
        return "redirect:/gestion/contenido/equipo";
    }

    private com.simbio.core.shared.security.AuthenticatedUser user() {
        return currentUser.currentUser().orElseThrow(com.simbio.core.content.application.ContentPermissionException::new);
    }
}
