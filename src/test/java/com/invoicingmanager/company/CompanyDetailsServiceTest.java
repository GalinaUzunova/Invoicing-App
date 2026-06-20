package com.invoicingmanager.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoicingmanager.user.UserEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.invocation.Invocation;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class CompanyDetailsServiceTest {

    @Mock
    private CompanyDetailsRepository companyDetailsRepository;

    @TempDir
    private Path uploadDir;

    @Test
    void getForUserReturnsExistingCompanyOrEmptyDetails() {
        UserEntity user = user(7L);
        CompanyDetailsEntity existing = company(user);
        when(companyDetailsRepository.findByUser(user))
                .thenReturn(Optional.of(existing))
                .thenReturn(Optional.empty());

        CompanyDetailsService service = service();

        assertThat(service.getForUser(user)).isSameAs(existing);
        assertThat(service.getForUser(user).getUser()).isSameAs(user);
    }

    @Test
    void saveCreatesCompanyDetailsAndTrimsFields() {
        UserEntity user = user(7L);
        when(companyDetailsRepository.findByUser(user)).thenReturn(Optional.empty());

        service().save(dto(), null, user);

        CompanyDetailsEntity savedCompany = savedCompany();
        assertThat(savedCompany.getUser()).isSameAs(user);
        assertThat(savedCompany.getCompanyName()).isEqualTo("Acme Ltd");
        assertThat(savedCompany.getVatNumber()).isEqualTo("GB123");
        assertThat(savedCompany.getEmail()).isEqualTo("info@acme.test");
    }

    @Test
    void saveThrowsExceptionWhenDtoIsNull() {
        UserEntity user = user(7L);

        assertThatThrownBy(() -> service().save(null, null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company details is required");
    }

    @Test
    void saveThrowsExceptionWhenUserIsNull() {
        CompanyDetailsDTO dto = dto();

        assertThatThrownBy(() -> service().save(dto, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
    }

    @Test
    void saveHandlesNullOptionalLogoGracefully() {
        UserEntity user = user(7L);
        when(companyDetailsRepository.findByUser(user)).thenReturn(Optional.empty());

        service().save(dto(), null, user);

        CompanyDetailsEntity savedCompany = savedCompany();
        assertThat(savedCompany).isNotNull();
        assertThat(savedCompany.getLogoFilename()).isNull();
    }

    @Test
    void saveTrimsRequiredFieldsAndPreservesNullOptionalDtoFields() {
        UserEntity user = user(7L);
        CompanyDetailsDTO dto = dto();
        dto.setVatNumber(null);
        dto.setAddress(null);
        when(companyDetailsRepository.findByUser(user)).thenReturn(Optional.empty());

        service().save(dto, null, user);

        CompanyDetailsEntity savedCompany = savedCompany();
        assertThat(savedCompany.getCompanyName()).isEqualTo("Acme Ltd");
        assertThat(savedCompany.getEmail()).isEqualTo("info@acme.test");
        assertThat(savedCompany.getPhone()).isEqualTo("+441234");
        assertThat(savedCompany.getVatNumber()).isNull();
        assertThat(savedCompany.getAddress()).isNull();
    }

    @Test
    void saveStoresLogoFileAndReturnsLogoResource() throws Exception {
        UserEntity user = user(7L);
        CompanyDetailsEntity company = company(user);
        MockMultipartFile logo = new MockMultipartFile(
                "logo",
                "logo.png",
                MediaType.IMAGE_PNG_VALUE,
                "png-data".getBytes()
        );
        when(companyDetailsRepository.findByUser(user)).thenReturn(Optional.of(company));

        service().save(dto(), logo, user);

        Path expectedLogo = uploadDir.resolve("logos").resolve("7").resolve("logo.png");
        assertThat(expectedLogo).exists();
        assertThat(Files.readString(expectedLogo)).isEqualTo("png-data");
        assertThat(company.getLogoFilename()).isEqualTo("logo.png");

        when(companyDetailsRepository.findByUser(user)).thenReturn(Optional.of(company));
        Optional<Resource> logoResource = service().getLogoResource(user);
        assertThat(logoResource).isPresent();
        assertThat(service().getLogoContentType(user)).contains(MediaType.IMAGE_PNG_VALUE);
    }

    @Test
    void saveRejectsUnsupportedLogoType() {
        UserEntity user = user(7L);
        CompanyDetailsEntity company = company(user);
        MockMultipartFile logo = new MockMultipartFile("logo", "logo.txt", MediaType.TEXT_PLAIN_VALUE, "text".getBytes());
        when(companyDetailsRepository.findByUser(user)).thenReturn(Optional.of(company));

        assertThatThrownBy(() -> service().save(dto(), logo, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Logo must be");
    }

    @Test
    void saveRejectsBlankRequiredCompanyFields() {
        UserEntity user = user(7L);
        CompanyDetailsDTO dto = dto();
        dto.setCompanyName(" ");
        when(companyDetailsRepository.findByUser(user)).thenReturn(Optional.of(company(user)));

        assertThatThrownBy(() -> service().save(dto, null, user))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company name is required");
    }

    @Test
    void removeLogoDeletesExistingFileAndClearsFilename() throws Exception {
        UserEntity user = user(7L);
        CompanyDetailsEntity company = company(user);
        company.setLogoFilename("logo.png");
        Path logoPath = uploadDir.resolve("logos").resolve("7").resolve("logo.png");
        Files.createDirectories(logoPath.getParent());
        Files.writeString(logoPath, "png-data");
        when(companyDetailsRepository.findByUser(user)).thenReturn(Optional.of(company));

        service().removeLogo(user);

        assertThat(logoPath).doesNotExist();
        assertThat(company.getLogoFilename()).isNull();
        verify(companyDetailsRepository).save(company);
    }

    @Test
    void requiredPublicMethodsRejectNullUser() {
        assertThatThrownBy(() -> service().getForUser(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
        assertThatThrownBy(() -> service().removeLogo(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
        assertThatThrownBy(() -> service().getLogoResource(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
        assertThatThrownBy(() -> service().getLogoContentType(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("user is required");
    }

    @Test
    void toDTOCopiesCompanyDetails() {
        CompanyDetailsEntity company = company(user(7L));
        company.setCompanyName("Acme Ltd");
        company.setVatNumber("GB123");
        company.setAddress("1 Main Street");
        company.setEmail("info@acme.test");
        company.setPhone("+441234");
        company.setLogoFilename("logo.png");

        CompanyDetailsDTO dto = service().toDTO(company);

        assertThat(dto.getCompanyName()).isEqualTo("Acme Ltd");
        assertThat(dto.getVatNumber()).isEqualTo("GB123");
        assertThat(dto.isHasLogo()).isTrue();
    }

    @Test
    void toDTOThrowsExceptionWhenCompanyIsNull() {
        assertThatThrownBy(() -> service().toDTO(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("company details is required");
    }

    @Test
    void constructorRejectsNullRepository() {
        assertThatThrownBy(() -> new CompanyDetailsService(null, uploadDir.toString()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("companyDetailsRepository");
    }

    @Test
    void constructorRejectsNullUploadDir() {
        assertThatThrownBy(() -> new CompanyDetailsService(companyDetailsRepository, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("uploadDir");
    }

    private CompanyDetailsService service() {
        return new CompanyDetailsService(companyDetailsRepository, uploadDir.toString());
    }

    private CompanyDetailsDTO dto() {
        CompanyDetailsDTO dto = new CompanyDetailsDTO();
        dto.setCompanyName(" Acme Ltd ");
        dto.setVatNumber(" GB123 ");
        dto.setAddress(" 1 Main Street ");
        dto.setEmail(" info@acme.test ");
        dto.setPhone(" +441234 ");
        return dto;
    }

    private CompanyDetailsEntity company(UserEntity user) {
        CompanyDetailsEntity company = new CompanyDetailsEntity();
        company.setUser(user);
        return company;
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail("owner@example.com");
        return user;
    }

    private CompanyDetailsEntity savedCompany() {
        return mockingDetails(companyDetailsRepository).getInvocations().stream()
                .filter(invocation -> "save".equals(invocation.getMethod().getName()))
                .findFirst()
                .map(this::companyArgument)
                .orElseThrow(() -> new AssertionError("Expected company details to be saved."));
    }

    private CompanyDetailsEntity companyArgument(Invocation invocation) {
        Object argument = invocation.getArgument(0);
        assertThat(argument).isInstanceOf(CompanyDetailsEntity.class);
        return (CompanyDetailsEntity) Objects.requireNonNull(argument, "saved company must not be null");
    }
}
