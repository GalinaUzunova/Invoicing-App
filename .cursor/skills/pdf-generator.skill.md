
---


**File:** `.cursor/skills/pdf-generator.skill.md`

```markdown
# Skill: PDF Invoice Generator

## Trigger: @pdf-gen

## Description
Generate professional PDF invoices using iTextPDF or OpenPDF.

## Features
- Convert invoice HTML to PDF
- Add company logo and branding
- Generate QR codes for payments
- Email-ready PDF attachments

## Usage
`@pdf-gen create PDF template for invoice`
`@pdf-gen generate invoice #12345 as PDF with company branding`

## Example Template
```java
@Service
public class PDFGeneratorService {
    public byte[] generateInvoicePDF(InvoiceEntity invoice) {
        // Uses Thymeleaf template + Flying Saucer to convert HTML to PDF
        // Includes: Header, customer info, line items, totals, payment terms
    }
}