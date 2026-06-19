package com.invoicingmanager.estimate;

import com.invoicingmanager.company.CompanyDetailsService;
import com.invoicingmanager.customer.CustomerService;
import com.invoicingmanager.pdf.DocumentPdfService;
import com.invoicingmanager.user.UserEntity;
import com.invoicingmanager.user.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/estimates")
public class EstimateController {

    private final EstimateService estimateService;
    private final CustomerService customerService;
    private final CompanyDetailsService companyDetailsService;
    private final DocumentPdfService documentPdfService;
    private final UserService userService;

    public EstimateController(
            EstimateService estimateService,
            CustomerService customerService,
            CompanyDetailsService companyDetailsService,
            DocumentPdfService documentPdfService,
            UserService userService
    ) {
        this.estimateService = estimateService;
        this.customerService = customerService;
        this.companyDetailsService = companyDetailsService;
        this.documentPdfService = documentPdfService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) EstimateStatus status, Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        model.addAttribute("estimates", estimateService.findAllForUser(user, status));
        model.addAttribute("statuses", EstimateStatus.values());
        model.addAttribute("selectedStatus", status);
        return "estimates/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long customerId, Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        prepareFormModel(model, user, "New Estimate", "/estimates");
        model.addAttribute("estimateDTO", estimateService.newEstimateDTO(customerId));
        return "estimates/form";
    }

    @PostMapping
    @SuppressWarnings("null")
    public String create(
            @Valid @ModelAttribute EstimateDTO estimateDTO,
            BindingResult bindingResult,
            Model model,
            Principal principal
    ) {
        UserEntity user = currentUser(principal);

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, user, "New Estimate", "/estimates");
            return "estimates/form";
        }

        try {
            EstimateEntity estimate = estimateService.create(estimateDTO, user);
            return "redirect:/estimates/" + estimate.getId();
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("quotationNumber", "quotationNumber.duplicate", Objects.toString(ex.getMessage(), "Invalid estimate."));
            prepareFormModel(model, user, "New Estimate", "/estimates");
            return "estimates/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        EstimateEntity estimate = estimateService.findByIdForUser(id, user);
        model.addAttribute("pageTitle", estimate.getQuotationNumber());
        model.addAttribute("estimate", estimate);
        model.addAttribute("company", companyDetailsService.getForUser(user));
        return "estimates/detail";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, Principal principal) {
        UserEntity user = currentUser(principal);
        EstimateEntity estimate = estimateService.findByIdForUser(id, user);
        byte[] pdf = documentPdfService.generateEstimatePdf(
                estimate,
                companyDetailsService.getForUser(user),
                logoDataUri(user)
        );

        return pdfResponse(pdf, "quotation-" + safeFilename(estimate.getQuotationNumber()) + ".pdf");
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        EstimateEntity estimate = estimateService.findByIdForUser(id, user);
        prepareFormModel(model, user, "Edit Estimate", "/estimates/" + id);
        model.addAttribute("estimateDTO", estimateService.toDTO(estimate));
        return "estimates/form";
    }

    @PostMapping("/{id}")
    @SuppressWarnings("null")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute EstimateDTO estimateDTO,
            BindingResult bindingResult,
            Model model,
            Principal principal
    ) {
        UserEntity user = currentUser(principal);

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, user, "Edit Estimate", "/estimates/" + id);
            return "estimates/form";
        }

        try {
            estimateService.update(id, estimateDTO, user);
            return "redirect:/estimates/" + id;
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("quotationNumber", "quotationNumber.duplicate", Objects.toString(ex.getMessage(), "Invalid estimate."));
            prepareFormModel(model, user, "Edit Estimate", "/estimates/" + id);
            return "estimates/form";
        }
    }

    @PostMapping("/{id}/send")
    public String send(@PathVariable Long id, Principal principal) {
        estimateService.markSent(id, currentUser(principal));
        return "redirect:/estimates/" + id;
    }

    @PostMapping("/{id}/accepted")
    public String markAccepted(@PathVariable Long id, Principal principal) {
        estimateService.markAccepted(id, currentUser(principal));
        return "redirect:/estimates/" + id;
    }

    @PostMapping("/{id}/declined")
    public String markDeclined(@PathVariable Long id, Principal principal) {
        estimateService.markDeclined(id, currentUser(principal));
        return "redirect:/estimates/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal) {
        estimateService.delete(id, currentUser(principal));
        return "redirect:/estimates";
    }

    private void prepareFormModel(Model model, UserEntity user, String pageTitle, String formAction) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
        model.addAttribute("customers", customerService.findAllForUser(user));
        model.addAttribute("statuses", EstimateStatus.values());
    }

    private UserEntity currentUser(Principal principal) {
        return userService.getCurrentUser(Objects.requireNonNull(principal, "principal must not be null").getName());
    }

    private String logoDataUri(UserEntity user) {
        return companyDetailsService.getLogoResource(user)
                .flatMap(resource -> companyDetailsService.getLogoContentType(user)
                        .flatMap(contentType -> documentPdfService.toPdfLogoDataUri(resource, contentType)))
                .orElse(null);
    }

    @SuppressWarnings("null")
    private ResponseEntity<byte[]> pdfResponse(byte[] pdf, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }

    private String safeFilename(String value) {
        if (value == null || value.isBlank()) {
            return "document";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
