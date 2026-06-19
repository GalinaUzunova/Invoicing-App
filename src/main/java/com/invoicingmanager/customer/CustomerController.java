package com.invoicingmanager.customer;

import com.invoicingmanager.invoice.InvoiceService;
import com.invoicingmanager.user.UserEntity;
import com.invoicingmanager.user.UserService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final InvoiceService invoiceService;
    private final UserService userService;

    public CustomerController(CustomerService customerService, InvoiceService invoiceService, UserService userService) {
        this.customerService = customerService;
        this.invoiceService = invoiceService;
        this.userService = userService;
    }

    @GetMapping
    public String list(Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        model.addAttribute("pageTitle", "Customers");
        model.addAttribute("customers", customerService.findAllForUser(user));
        return "customers/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "New Customer");
        model.addAttribute("customerDTO", new CustomerDTO());
        model.addAttribute("formAction", "/customers");
        return "customers/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute CustomerDTO customerDTO,
            BindingResult bindingResult,
            Model model,
            Principal principal
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "New Customer");
            model.addAttribute("formAction", "/customers");
            return "customers/form";
        }

        CustomerEntity customer = customerService.create(customerDTO, currentUser(principal));
        return "redirect:/customers/" + customer.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        CustomerEntity customer = customerService.findByIdForUser(id, user);
        model.addAttribute("pageTitle", customer.getName());
        model.addAttribute("customer", customer);
        model.addAttribute("invoices", invoiceService.findByCustomer(customer, user));
        return "customers/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        UserEntity user = currentUser(principal);
        CustomerEntity customer = customerService.findByIdForUser(id, user);
        model.addAttribute("pageTitle", "Edit Customer");
        model.addAttribute("customerDTO", customerService.toDTO(customer));
        model.addAttribute("formAction", "/customers/" + id);
        return "customers/form";
    }

    @PostMapping("/{id}")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute CustomerDTO customerDTO,
            BindingResult bindingResult,
            Model model,
            Principal principal
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "Edit Customer");
            model.addAttribute("formAction", "/customers/" + id);
            return "customers/form";
        }

        customerService.update(id, customerDTO, currentUser(principal));
        return "redirect:/customers/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Principal principal) {
        customerService.delete(id, currentUser(principal));
        return "redirect:/customers";
    }

    private UserEntity currentUser(Principal principal) {
        return userService.getCurrentUser(Objects.requireNonNull(principal, "principal must not be null").getName());
    }
}
