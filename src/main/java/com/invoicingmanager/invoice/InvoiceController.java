package com.invoicingmanager.invoice;

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
@RequestMapping("/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final CustomerService customerService;
    private final CompanyDetailsService companyDetailsService;
    private final DocumentPdfService documentPdfService;
    private final UserService userService;

    public InvoiceController(
            InvoiceService invoiceService,
            CustomerService customerService,
            CompanyDetailsService companyDetailsService,
            DocumentPdfService documentPdfService,
            UserService userService
    ) {
        this.invoiceService = invoiceService;
        this.customerService = customerService;
        this.companyDetailsService = companyDetailsService;
        this.documentPdfService = documentPdfService;
        this.userService = userService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) InvoiceStatus status, Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        model.addAttribute("pageTitle", "Invoices");
        model.addAttribute("invoices", invoiceService.findAllForUser(user, status));
        model.addAttribute("statuses", InvoiceStatus.values());
        model.addAttribute("selectedStatus", status);
        return "invoices/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long customerId, Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        prepareFormModel(model, user, "New Invoice", "/invoices");
        model.addAttribute("invoiceDTO", invoiceService.newInvoiceDTO(customerId));
        return "invoices/form";
    }

    @PostMapping
    @SuppressWarnings("null")
    public String create(
            @Valid @ModelAttribute InvoiceDTO invoiceDTO,
            BindingResult bindingResult,
            Model model,
            Principal principal
    ) {
        UserEntity user = currentUser(principal);

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, user, "New Invoice", "/invoices");
            return "invoices/form";
        }

        try {
            InvoiceEntity invoice = invoiceService.create(invoiceDTO, user);
            return "redirect:/invoices/" + invoice.getId();
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("invoiceNumber", "invoiceNumber.duplicate", Objects.toString(exception.getMessage(), "Invalid invoice."));
            prepareFormModel(model, user, "New Invoice", "/invoices");
            return "invoices/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        InvoiceEntity invoice = invoiceService.findByIdForUser(id, user);
        model.addAttribute("pageTitle", invoice.getInvoiceNumber());
        model.addAttribute("invoice", invoice);
        model.addAttribute("company", companyDetailsService.getForUser(user));
        return "invoices/detail";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable Long id, Principal principal) {
        UserEntity user = currentUser(principal);
        InvoiceEntity invoice = invoiceService.findByIdForUser(id, user);
        byte[] pdf = documentPdfService.generateInvoicePdf(
                invoice,
                companyDetailsService.getForUser(user),
                logoDataUri(user)
        );

        return pdfResponse(pdf, "invoice-" + safeFilename(invoice.getInvoiceNumber()) + ".pdf");
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        InvoiceEntity invoice = invoiceService.findByIdForUser(id, user);
        prepareFormModel(model, user, "Edit Invoice", "/invoices/" + id);
        model.addAttribute("invoiceDTO", invoiceService.toDTO(invoice));
        return "invoices/form";
    }

    @PostMapping("/{id}")
    @SuppressWarnings("null")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute InvoiceDTO invoiceDTO,
            BindingResult bindingResult,
            Model model,
            Principal principal
    ) {
        UserEntity user = currentUser(principal);

        if (bindingResult.hasErrors()) {
            prepareFormModel(model, user, "Edit Invoice", "/invoices/" + id);
            return "invoices/form";
        }

        try {
            invoiceService.update(id, invoiceDTO, user);
            return "redirect:/invoices/" + id;
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("invoiceNumber", "invoiceNumber.duplicate", Objects.toString(exception.getMessage(), "Invalid invoice."));
            prepareFormModel(model, user, "Edit Invoice", "/invoices/" + id);
            return "invoices/form";
        }
    }

    @PostMapping("/{id}/send")
    public String send(@PathVariable Long id, Principal principal) {
        invoiceService.markSent(id, currentUser(principal));
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/{id}/paid")
    public String markPaid(@PathVariable Long id, Principal principal) {
        invoiceService.markPaid(id, currentUser(principal));
        return "redirect:/invoices/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal) {
        invoiceService.delete(id, currentUser(principal));
        return "redirect:/invoices";
    }

    private void prepareFormModel(Model model, UserEntity user, String pageTitle, String formAction) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("formAction", formAction);
        model.addAttribute("customers", customerService.findAllForUser(user));
        model.addAttribute("statuses", InvoiceStatus.values());
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
