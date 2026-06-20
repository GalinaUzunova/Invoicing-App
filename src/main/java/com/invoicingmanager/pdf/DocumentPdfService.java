package com.invoicingmanager.pdf;

import com.invoicingmanager.company.CompanyDetailsEntity;
import com.invoicingmanager.estimate.EstimateEntity;
import com.invoicingmanager.invoice.InvoiceEntity;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class DocumentPdfService {

    private static final Set<String> PDF_LOGO_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif"
    );

    private final TemplateEngine templateEngine;

    public DocumentPdfService(TemplateEngine templateEngine) {
        this.templateEngine = Objects.requireNonNull(templateEngine, "templateEngine must not be null");
    }

    public byte[] generateInvoicePdf(InvoiceEntity invoice, CompanyDetailsEntity company, String logoDataUri) {
        InvoiceEntity requiredInvoice = requireArgument(invoice, "invoice");
        CompanyDetailsEntity requiredCompany = requireArgument(company, "company details");
        Context context = new Context();
        context.setVariable("invoice", requiredInvoice);
        context.setVariable("company", requiredCompany);
        context.setVariable("logoDataUri", logoDataUri);
        return render("pdf/invoice", context);
    }

    public byte[] generateEstimatePdf(EstimateEntity estimate, CompanyDetailsEntity company, String logoDataUri) {
        EstimateEntity requiredEstimate = requireArgument(estimate, "estimate");
        CompanyDetailsEntity requiredCompany = requireArgument(company, "company details");
        Context context = new Context();
        context.setVariable("estimate", requiredEstimate);
        context.setVariable("company", requiredCompany);
        context.setVariable("logoDataUri", logoDataUri);
        return render("pdf/estimate", context);
    }

    public Optional<String> toPdfLogoDataUri(Resource resource, String contentType) {
        if (contentType == null || !PDF_LOGO_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }

        try {
            byte[] bytes = requireArgument(resource, "logo resource").getInputStream().readAllBytes();
            return Optional.of("data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes));
        } catch (IOException exception) {
            log.warn("Unable to read PDF logo resource with content type {}", contentType, exception);
            return Optional.empty();
        }
    }

    private byte[] render(String templateName, Context context) {
        String html = templateEngine.process(templateName, context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            log.error("Unable to generate PDF document from template {}", templateName, exception);
            throw new IllegalStateException("Unable to generate PDF document.", exception);
        }
    }

    private <T> T requireArgument(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return value;
    }
}
