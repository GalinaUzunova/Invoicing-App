package com.invoicingmanager.company;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.invoicingmanager.security.SecurityConfig;
import com.invoicingmanager.user.UserEntity;
import com.invoicingmanager.user.UserService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CompanyDetailsController.class)
@Import(SecurityConfig.class)
class CompanyDetailsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyDetailsService companyDetailsService;

    @MockitoBean
    private UserService userService;

    @Test
    void companyPageRedirectsAnonymousUsersToLogin() throws Exception {
        mockMvc.perform(get("/company"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void formShowsCompanyDetails() throws Exception {
        UserEntity user = new UserEntity();
        CompanyDetailsEntity company = new CompanyDetailsEntity();
        CompanyDetailsDTO dto = new CompanyDetailsDTO();
        dto.setCompanyName("Acme Ltd");
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(companyDetailsService.getForUser(user)).thenReturn(company);
        when(companyDetailsService.toDTO(company)).thenReturn(dto);

        mockMvc.perform(get("/company"))
                .andExpect(status().isOk())
                .andExpect(view().name("company/form"))
                .andExpect(model().attributeExists("companyDetailsDTO"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void saveCompanyDetailsRedirectsAfterSuccessfulPost() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.png", MediaType.IMAGE_PNG_VALUE, "png".getBytes());

        mockMvc.perform(multipart("/company")
                        .file(logo)
                        .param("companyName", "Acme Ltd")
                        .param("email", "info@acme.test")
                        .param("phone", "+441234")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/company"));
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void saveCompanyDetailsRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/company")
                        .param("companyName", "Acme Ltd"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void logoEndpointReturnsSavedLogo() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(companyDetailsService.getLogoResource(user)).thenReturn(Optional.of(new ByteArrayResource("png".getBytes())));
        when(companyDetailsService.getLogoContentType(user)).thenReturn(Optional.of(MediaType.IMAGE_PNG_VALUE));

        mockMvc.perform(get("/company/logo"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "owner@example.com")
    void missingLogoReturnsNotFound() throws Exception {
        UserEntity user = new UserEntity();
        when(userService.getCurrentUser("owner@example.com")).thenReturn(user);
        when(companyDetailsService.getLogoResource(user)).thenReturn(Optional.empty());

        mockMvc.perform(get("/company/logo"))
                .andExpect(status().isNotFound());
    }
}
