package com.invoicingmanager.company;

import com.invoicingmanager.user.UserEntity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    public CompanyDetailsEntity getForUser(UserEntity user) {
        UserEntity requiredUser = requireArgument(user, "user");
        return companyDetailsRepository.findByUser(requiredUser).orElseGet(() -> createEmpty(requiredUser));
    }

    @Transactional
    public CompanyDetailsEntity save(CompanyDetailsDTO dto, MultipartFile logo, UserEntity user) {
        CompanyDetailsDTO requiredDto = requireArgument(dto, "company details");
        UserEntity requiredUser = requireArgument(user, "user");
        CompanyDetailsEntity company = companyDetailsRepository.findByUser(requiredUser)
                .orElseGet(() -> createEmpty(requiredUser));

        company.setCompanyName(trimRequired(requiredDto.getCompanyName(), "Company name"));
        company.setVatNumber(trim(requiredDto.getVatNumber()));
        company.setAddress(trim(requiredDto.getAddress()));
        company.setEmail(trimRequired(requiredDto.getEmail(), "Company email"));
        company.setPhone(trimRequired(requiredDto.getPhone(), "Company phone"));

        if (logo != null && !logo.isEmpty()) {
            storeLogo(company, logo, requiredUser);
        }

        return companyDetailsRepository.save(company);
    }

    @Transactional
    public void removeLogo(UserEntity user) {
        UserEntity requiredUser = requireArgument(user, "user");
        companyDetailsRepository.findByUser(requiredUser)
                .filter(CompanyDetailsEntity::hasLogo)
                .ifPresent(company -> {
                    deleteLogoFile(requiredUser, company.getLogoFilename());
                    company.setLogoFilename(null);
                    companyDetailsRepository.save(company);
                });
    }

    @Transactional(readOnly = true)
    public Optional<Resource> getLogoResource(UserEntity user) {
        UserEntity requiredUser = requireArgument(user, "user");
        return companyDetailsRepository.findByUser(requiredUser)
                .filter(CompanyDetailsEntity::hasLogo)
                .flatMap(company -> logoResource(requiredUser, company))
                .filter(Resource::exists);
    }

    @Transactional(readOnly = true)
    public Optional<String> getLogoContentType(UserEntity user) {
        UserEntity requiredUser = requireArgument(user, "user");
        return companyDetailsRepository.findByUser(requiredUser)
                .filter(CompanyDetailsEntity::hasLogo)
                .flatMap(company -> Optional.ofNullable(company.getLogoFilename()))
                .map(this::contentTypeForFilename);
    }

    public CompanyDetailsDTO toDTO(CompanyDetailsEntity company) {
        CompanyDetailsEntity requiredCompany = requireArgument(company, "company details");
        CompanyDetailsDTO dto = new CompanyDetailsDTO();
        dto.setCompanyName(requiredCompany.getCompanyName());
        dto.setVatNumber(requiredCompany.getVatNumber());
        dto.setAddress(requiredCompany.getAddress());
        dto.setEmail(requiredCompany.getEmail());
        dto.setPhone(requiredCompany.getPhone());
        dto.setHasLogo(requiredCompany.hasLogo());
        return dto;
    }

    private CompanyDetailsEntity createEmpty(UserEntity user) {
        CompanyDetailsEntity company = new CompanyDetailsEntity();
        company.setUser(user);
        return company;
    }

    private void storeLogo(CompanyDetailsEntity company, MultipartFile logo, UserEntity user) {
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
            log.error("Unable to save company logo for user {}", user.getEmail(), exception);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save logo.", exception);
        }
    }

    private Path logoDirectory(UserEntity user) {
        return uploadRoot.resolve("logos").resolve(String.valueOf(user.getId()));
    }

    private Path logoPath(UserEntity user, String filename) {
        return logoDirectory(user).resolve(filename);
    }

    private Optional<Resource> logoResource(UserEntity user, CompanyDetailsEntity company) {
        return Optional.ofNullable(company.getLogoFilename())
                .map(filename -> toLogoResource(user, filename));
    }

    private Resource toLogoResource(UserEntity user, String filename) {
        Path path = Objects.requireNonNull(logoPath(user, filename), "logo path must not be null");
        return new FileSystemResource(path);
    }

    private void deleteLogoFile(UserEntity user, String filename) {
        String requiredFilename = requireArgument(filename, "logo filename");
        try {
            Files.deleteIfExists(logoPath(user, requiredFilename));
        } catch (IOException exception) {
            log.error("Unable to delete company logo {} for user {}", requiredFilename, user.getEmail(), exception);
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

    private <T> T requireArgument(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
