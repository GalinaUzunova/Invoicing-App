package com.invoicingmanager.dashboard;

import com.invoicingmanager.user.UserEntity;
import com.invoicingmanager.user.UserService;
import java.security.Principal;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserService userService;

    public DashboardController(DashboardService dashboardService, UserService userService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal) {
        UserEntity user = userService.getCurrentUser(Objects.requireNonNull(principal, "principal must not be null").getName());
        model.addAttribute("pageTitle", "Dashboard");
        model.addAttribute("summary", dashboardService.getSummary(user));
        model.addAttribute("recentInvoices", dashboardService.getRecentInvoices(user));
        return "dashboard/index";
    }
}
