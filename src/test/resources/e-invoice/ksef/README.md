# Usage Example

```java
// Load template
InputStream template = getClass().getResourceAsStream("/FA_VAT_FA3_template.xml");
Document doc = EInvoiceFactory.loadDocument(template);

// Create KSeF model
EInvoiceModelKSeF invoice = new EInvoiceModelKSeF(doc);

// Set invoice data
invoice.setId("INV-2025-001234");
invoice.setIssueDateTime(LocalDate.now());
invoice.setDueDateTime(LocalDate.now().plusDays(14));

// Set buyer (customer)
invoice.setBuyer(
    "9876543210",              // NIP
    "Customer Company Ltd.",   // Name
    "Customer Street",         // Street
    "456",                     // House number
    "60-001",                  // Postal code
    "Poznan",                  // City
    "PL"                       // Country
);

// Add line items with PKWiU codes
invoice.addLineItem(
    1,                         // Line number
    "Transport Hamburg-Warsaw", // Description
    "86.10.Z",                 // PKWiU code (MANDATORY!)
    1.0,                       // Quantity
    1000.00,                   // Unit price
    1000.00,                   // Net amount
    23                         // VAT rate 23%
);

// Set totals
invoice.setNetTotalAmount(new BigDecimal("1000.00"));
invoice.setTaxTotalAmount(new BigDecimal("230.00"));
invoice.setGrandTotalAmount(new BigDecimal("1230.00"));

// Save to file
byte[] xmlBytes = EInvoiceFactory.saveDocument(invoice.getDoc());
Files.write(Paths.get("FA1.xml"), xmlBytes);
```
