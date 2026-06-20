package com.invoicingmanager.company;

import com.invoicingmanager.user.UserEntity;
import com.invoicingmanager.user.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Slf4j
@RequestMapping("/company")
public class CompanyDetailsController {

    private final CompanyDetailsService companyDetailsService;
    private final UserService userService;

    public CompanyDetailsController(CompanyDetailsService companyDetailsService, UserService userService) {
        this.companyDetailsService = companyDetailsService;
        this.userService = userService;
    }

    @GetMapping
    public String form(Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        CompanyDetailsEntity company = companyDetailsService.getForUser(user);
        model.addAttribute("companyDetailsDTO", companyDetailsService.toDTO(company));
        return "company/form";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute CompanyDetailsDTO companyDetailsDTO,
            @RequestParam(value = "logo", required = false) MultipartFile logo,
            BindingResult bindingResult,
            Model model,
            Principal principal,
            RedirectAttributes redirectAttributes
    ) {
        UserEntity user = currentUser(principal);

        if (bindingResult.hasErrors()) {
            return "company/form";
        }

        try {
            companyDetailsService.save(companyDetailsDTO, logo, user);
            redirectAttributes.addFlashAttribute("saved", true);
            return "redirect:/company";
        } catch (IllegalArgumentException exception) {
            String message = exceptionMessage(exception, "Invalid company details.");
            log.warn("Company details update failed for user {}: {}", user.getEmail(), message);
            bindingResult.reject("logo.invalid", Objects.requireNonNull(message, "message must not be null"));
            return "company/form";
        }
    }

    @PostMapping("/logo/remove")
    public String removeLogo(Principal principal, RedirectAttributes redirectAttributes) {
        companyDetailsService.removeLogo(currentUser(principal));
        redirectAttributes.addFlashAttribute("saved", true);
        return "redirect:/company";
    }

    @GetMapping("/logo")
    public ResponseEntity<Resource> logo(Principal principal) {
        UserEntity user = currentUser(principal);
        return companyDetailsService.getLogoResource(user)
                .map(resource -> ResponseEntity.ok()
                        .contentType(Objects.requireNonNull(logoContentType(user), "logo media type must not be null"))
                        .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                        .body(resource))
                .orElse(ResponseEntity.notFound().build());
    }

    private UserEntity currentUser(Principal principal) {
        return userService.getCurrentUser(Objects.requireNonNull(principal, "principal must not be null").getName());
    }

    private MediaType logoContentType(UserEntity user) {
        String contentType = companyDetailsService.getLogoContentType(user)
                .orElse(Objects.requireNonNull(MediaType.IMAGE_PNG_VALUE, "PNG media type must not be null"));
        return MediaType.parseMediaType(Objects.requireNonNull(contentType, "logo content type must not be null"));
    }

    private String exceptionMessage(RuntimeException exception, String fallback) {
        return Optional.ofNullable(exception.getMessage())
                .filter(message -> !message.isBlank())
                .orElse(fallback);
    }
}
