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
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
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
        Context context = new Context();
        context.setVariable("invoice", invoice);
        context.setVariable("company", company);
        context.setVariable("logoDataUri", logoDataUri);
        return render("pdf/invoice", context);
    }

    public byte[] generateEstimatePdf(EstimateEntity estimate, CompanyDetailsEntity company, String logoDataUri) {
        Context context = new Context();
        context.setVariable("estimate", estimate);
        context.setVariable("company", company);
        context.setVariable("logoDataUri", logoDataUri);
        return render("pdf/estimate", context);
    }

    public Optional<String> toPdfLogoDataUri(Resource resource, String contentType) {
        if (contentType == null || !PDF_LOGO_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return Optional.empty();
        }

        try {
            byte[] bytes = resource.getInputStream().readAllBytes();
            return Optional.of("data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes));
        } catch (IOException exception) {
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
            throw new IllegalStateException("Unable to generate PDF document.", exception);
        }
    }
}
