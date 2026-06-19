package com.invoicingmanager.company;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.invoicingmanager.user.UserEntity;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        when(companyDetailsRepository.findByUser(user)).thenReturn(Optional.of(existing), Optional.empty());

        CompanyDetailsService service = service();

        assertThat(service.getForUser(user)).isSameAs(existing);
        assertThat(service.getForUser(user).getUser()).isSameAs(user);
    }

    @Test
    void saveCreatesCompanyDetailsAndTrimsFields() {
        UserEntity user = user(7L);
        when(companyDetailsRepository.findByUser(user)).thenReturn(Optional.empty());
        when(companyDetailsRepository.save(any(CompanyDetailsEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyDetailsEntity saved = service().save(dto(), null, user);

        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getCompanyName()).isEqualTo("Acme Ltd");
        assertThat(saved.getVatNumber()).isEqualTo("GB123");

        ArgumentCaptor<CompanyDetailsEntity> captor = ArgumentCaptor.forClass(CompanyDetailsEntity.class);
        verify(companyDetailsRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("info@acme.test");
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
        when(companyDetailsRepository.save(company)).thenReturn(company);

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
}
