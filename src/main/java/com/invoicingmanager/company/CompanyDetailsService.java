package com.invoicingmanager.company;

import com.invoicingmanager.user.UserEntity;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompanyDetailsService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp"
    );

    private final CompanyDetailsRepository companyDetailsRepository;
    private final Path uploadRoot;

    public CompanyDetailsService(
            CompanyDetailsRepository companyDetailsRepository,
            @Value("${app.upload.dir:uploads}") String uploadDir
    ) {
        this.companyDetailsRepository = Objects.requireNonNull(companyDetailsRepository, "companyDetailsRepository must not be null");
        this.uploadRoot = Path.of(Objects.requireNonNull(uploadDir, "uploadDir must not be null")).toAbsolutePath().normalize();
    }

    @Transactional(readOnly = true)
    public CompanyDetailsEntity getForUser(@NotNull UserEntity user) {
        Objects.requireNonNull(user, "user must not be null");
        return companyDetailsRepository.findByUser(user).orElseGet(() -> emptyDetails(user));
    }

    @Transactional
    public CompanyDetailsEntity save(@NotNull CompanyDetailsDTO dto, MultipartFile logo, @NotNull UserEntity user) {
        Objects.requireNonNull(dto, "dto must not be null");
        Objects.requireNonNull(user, "user must not be null");
        CompanyDetailsEntity company = companyDetailsRepository.findByUser(user)
                .orElseGet(() -> createEmpty(user));

        company.setCompanyName(trimRequired(dto.getCompanyName(), "Company name"));
        company.setVatNumber(trim(dto.getVatNumber()));
        company.setAddress(trim(dto.getAddress()));
        company.setEmail(trimRequired(dto.getEmail(), "Company email"));
        company.setPhone(trimRequired(dto.getPhone(), "Company phone"));

        if (logo != null && !logo.isEmpty()) {
            storeLogo(company, logo, user);
        }

        return companyDetailsRepository.save(company);
    }

    @Transactional
    public void removeLogo(@NotNull UserEntity user) {
        Objects.requireNonNull(user, "user must not be null");
        CompanyDetailsEntity company = companyDetailsRepository.findByUser(user).orElse(null);
        if (company == null || !company.hasLogo()) {
            return;
        }

        deleteLogoFile(user, company.getLogoFilename());
        company.setLogoFilename(null);
        companyDetailsRepository.save(company);
    }

    @Transactional(readOnly = true)
    public Optional<Resource> getLogoResource(@NotNull UserEntity user) {
        Objects.requireNonNull(user, "user must not be null");
        return companyDetailsRepository.findByUser(user)
                .filter(CompanyDetailsEntity::hasLogo)
                .map(company -> (Resource) new FileSystemResource(logoPath(user, company.getLogoFilename())))
                .filter(Resource::exists);
    }

    @Transactional(readOnly = true)
    public Optional<String> getLogoContentType(@NotNull UserEntity user) {
        Objects.requireNonNull(user, "user must not be null");
        return companyDetailsRepository.findByUser(user)
                .filter(CompanyDetailsEntity::hasLogo)
                .map(company -> contentTypeForFilename(company.getLogoFilename()));
    }

    public CompanyDetailsDTO toDTO(@NotNull CompanyDetailsEntity company) {
        Objects.requireNonNull(company, "company must not be null");
        CompanyDetailsDTO dto = new CompanyDetailsDTO();
        dto.setCompanyName(company.getCompanyName());
        dto.setVatNumber(company.getVatNumber());
        dto.setAddress(company.getAddress());
        dto.setEmail(company.getEmail());
        dto.setPhone(company.getPhone());
        dto.setHasLogo(company.hasLogo());
        return dto;
    }

    private CompanyDetailsEntity createEmpty(UserEntity user) {
        Objects.requireNonNull(user, "user must not be null");
        CompanyDetailsEntity company = new CompanyDetailsEntity();
        company.setUser(user);
        return company;
    }

    private CompanyDetailsEntity emptyDetails(UserEntity user) {
        Objects.requireNonNull(user, "user must not be null");
        CompanyDetailsEntity company = new CompanyDetailsEntity();
        company.setUser(user);
        return company;
    }

    private void storeLogo(CompanyDetailsEntity company, MultipartFile logo, UserEntity user) {
        Objects.requireNonNull(company, "company must not be null");
        Objects.requireNonNull(logo, "logo must not be null");
        Objects.requireNonNull(user, "user must not be null");
        String contentType = logo.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Logo must be a JPEG, PNG, GIF, or WebP image.");
        }

        if (logo.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("Logo must be 2 MB or smaller.");
        }

        String extension = extensionForContentType(contentType);
        String filename = "logo." + extension;
        Path userLogoDir = logoDirectory(user);

        try {
            Files.createDirectories(userLogoDir);
            if (company.hasLogo()) {
                deleteLogoFile(user, company.getLogoFilename());
            }
            Path destination = userLogoDir.resolve(filename);
            Files.copy(logo.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
            company.setLogoFilename(filename);
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save logo.", exception);
        }
    }

    private Path logoDirectory(UserEntity user) {
        Objects.requireNonNull(user, "user must not be null");
        return uploadRoot.resolve("logos").resolve(String.valueOf(user.getId()));
    }

    private Path logoPath(UserEntity user, String filename) {
        Objects.requireNonNull(filename, "filename must not be null");
        return logoDirectory(user).resolve(filename);
    }

    private void deleteLogoFile(UserEntity user, String filename) {
        try {
            Files.deleteIfExists(logoPath(user, filename));
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete logo.", exception);
        }
    }

    private String extensionForContentType(String contentType) {
        return switch (contentType.toLowerCase(Locale.ROOT)) {
            case MediaType.IMAGE_JPEG_VALUE -> "jpg";
            case MediaType.IMAGE_PNG_VALUE -> "png";
            case MediaType.IMAGE_GIF_VALUE -> "gif";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }

    private String contentTypeForFilename(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF_VALUE;
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimRequired(String value, String fieldName) {
        String trimmed = trim(value);
        if (trimmed == null || trimmed.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return trimmed;
    }
}
