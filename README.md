# e-invoice

Imixs e-invoice is a lightweight and efficient Java library for processing
e-invoices documents. The library stands out for its independence from external
dependencies and seamlessly integrates into modern Java projects. It supports
the major European e-invoice standards **Factur-X (CII)**, **UBL** and the
Polish **KSeF FA(3)** format.

## Key Features

- Native Java support, integration without external dependencies
- Support for e-invoice formats Factur-X (CII), UBL and KSeF FA(3)
- Read **and** write CII and KSeF documents
- Simple and intuitive API for developers
- Extensible model API - implement your own e-invoice format
- Open Source under MIT license

## Format Support

| Format                   | Namespace                              | Read | Write |
| ------------------------ | -------------------------------------- | :--: | :---: |
| Factur-X / ZUGFeRD (CII) | `CrossIndustryInvoice` (UN/CEFACT CII) |  ✅  |  ✅   |
| UBL (BIS 3)              | `urn:oasis:names:specification:ubl...` |  ✅  | ➖ \* |
| KSeF FA(3) (Poland)      | `http://crd.gov.pl/...`                |  ✅  |  ✅   |

\* UBL is currently read-only. See [Contributing](#how-to-join-this-project) if
you want to help implement UBL write support.

## Use Cases

The Imixs e-invoice library serves as a simple to use solution for parsing and
writing XML e-invoice documents. You can easily leverage the library for
building automated invoice processing systems and implementing export functions
for standardized electronic invoices. Its support for multiple e-invoice formats
makes it easy to use for projects involving automated processing and format
migration, helping businesses adapt to different trading partner requirements.

## Technical Details

The library focuses on XML processing of e-invoices.

- For **ZUGFeRD-compliant PDF documents**, the generated XML must be manually
  embedded into the PDF file.
- Reading and writing of **Factur-X (CII)** invoices is fully supported.
- **UBL** is currently available in **read mode only**.
- **KSeF FA(3)** is supported in read and write mode.

### Design Note: Field Semantics vs. XML Mapping

The library separates the **invoice data model** (`TradeLineItem`,
`TradeParty`, header fields on `EInvoiceModel`) from the **format-specific XML
mapping** (`EInvoiceModelCII`, `EInvoiceModelUBL`, `EInvoiceModelKSeF`).

Each concrete model implementation decides how the shared model fields are
mapped onto its own XML structure. This makes it possible to implement
additional e-invoice formats on top of the same model API - see
[Implementing a custom format](#implementing-a-custom-format).

## How to use

Integration into Maven projects is done by adding the following dependency:

```xml
<dependency>
    <groupId>org.imixs.util</groupId>
    <artifactId>imixs-e-invoice</artifactId>
    <version>{VERSION}</version>
</dependency>
```

You can find the latest version in the [release notes](https://github.com/imixs/e-invoice/releases).

## Reading and Parsing an E-Invoice

The `EInvoiceModelFactory` detects the format automatically from the root
element and its namespace, and returns the matching model implementation.

```java
EInvoiceModel eInvoiceModel = null;
try (InputStream is = myInputStream) {
    if (is == null) {
        throw new IOException("Resource not found");
    }
    eInvoiceModel = EInvoiceModelFactory.read(is);
}

// Verify the result
assertNotNull(eInvoiceModel);
assertEquals("R-00010", eInvoiceModel.getId());

LocalDate invoiceDate = eInvoiceModel.getIssueDateTime();
assertEquals(LocalDate.of(2021, 7, 28), invoiceDate);

assertEquals(new BigDecimal("4380.90"), eInvoiceModel.getGrandTotalAmount());
assertEquals(new BigDecimal("510.90"),  eInvoiceModel.getTaxTotalAmount());
assertEquals(new BigDecimal("3870.00"), eInvoiceModel.getNetTotalAmount());

// Trade parties
TradeParty seller = eInvoiceModel.findTradeParty("seller");
assertNotNull(seller);
assertEquals("Max Mustermann", seller.getName());
assertEquals("DE111111111", seller.getVatNumber());

// Line items
for (TradeLineItem item : eInvoiceModel.getTradeLineItems()) {
    System.out.println(item.getId() + " " + item.getName()
        + " net=" + item.getNetPrice()
        + " total=" + item.getTotal()
        + " vat=" + item.getTaxRate());
}
```

> **Note:** `EInvoiceModelFactory.read(...)` throws
> `EInvoiceFormatException` if the document is not a supported e-invoice
> format, and `NullPointerException` if the `InputStream` is `null`.

You can find the full example in the JUnit test package of this project.

## Creating an E-Invoice Document

To create a new e-invoice document, start from any valid e-invoice template as
an XML file. The template is the base for the core model that can be updated by
the library.

```java
// 1. Read an existing XML template
EInvoiceModel model = EInvoiceModelFactory.read(new ByteArrayInputStream(myXMLTemplate));

// 2. Update header data
model.setId("R-10000");
model.setIssueDateTime(LocalDate.of(2025, 3, 10));
model.setDueDateTime(LocalDate.of(2025, 4, 10));

// 3. Set the seller (required)
TradeParty seller = new TradeParty("seller");
seller.setName("Imixs Software Solutions GmbH");
seller.setStreetAddress("Lindenstr. 1");
seller.setPostcodeCode("10823");
seller.setCityName("Berlin");
seller.setCountryId("DE");
seller.setVatNumber("DE111111111");
model.setTradeParty(seller);

// 4. Set the buyer
TradeParty buyer = new TradeParty("buyer");
buyer.setName("Max Mustermann");
buyer.setStreetAddress("Hauptstr. 5");
buyer.setPostcodeCode("10115");
buyer.setCityName("Berlin");
buyer.setCountryId("DE");
model.setTradeParty(buyer);

// 5. Add line items - one TradeLineItem per invoice position
TradeLineItem item = new TradeLineItem("1");
item.setName("Moon Rocket");
item.setDescription("Fly me to the moon");
item.setQuantity(1.0);
item.setGrossPrice(1000000.00);   // list price
item.setNetPrice(840336.13);      // net unit price
item.setTaxRate(19.0);            // VAT rate in percent
item.setTotal(840336.13);         // NET total of this line (BT-131)
model.addTradeLineItem(item);

// 6. Serialize back to XML
byte[] myEInvoice = model.getContent();
```

### Important: how totals are calculated

Since version 1.0.3 the **header totals are always derived from the line
items**. Whenever you call `addTradeLineItem(...)`, the model automatically:

1. recalculates `netTotalAmount`, `taxTotalAmount` and `grandTotalAmount`
   from the line items,
2. builds a **per-rate VAT breakdown** (BG-23) - one tax block per distinct
   VAT rate,
3. writes the totals and the VAT breakdown into the format-specific XML.

This means: **you do not need to set the header totals manually** if you build an invoice
from line items. The following calls exist, but their values will be
overwritten on the next `addTradeLineItem(...)`:

```java
// Only relevant when reading an existing document.
model.getNetTotalAmount();
model.getTaxTotalAmount();
model.getGrandTotalAmount();
```

### TradeLineItem field reference

| Field              | Meaning                                              | EN16931 |
| ------------------ | ---------------------------------------------------- | ------- |
| `id`               | Line position / line number                          | BT-126  |
| `name`             | Item name                                            | BT-153  |
| `description`      | Item description                                     | BT-154  |
| `quantity`         | Billed quantity                                      | BT-129  |
| `grossPrice`       | List price (before discounts)                        | BT-148  |
| `netPrice`         | Net unit price actually charged                      | BT-146  |
| `taxRate`          | VAT rate in percent (e.g. `19.0`)                    | BT-152  |
| `total`            | **Net** total amount of this line (`netPrice × qty`) | BT-131  |
| `orderReferenceId` | Optional order line reference                        | BT-132  |

> **`total` is a NET amount**, not a gross amount. It maps to the line total
> (`LineTotalAmount` in CII, `LineExtensionAmount` in UBL, `P_11` in KSeF).
> If you want to express a gross line total, compute it yourself and use the
> correct VAT rate - the library will add the tax on top when recalculating
> the header totals.

## Implementing a Custom Format

The model layer is intentionally kept format-agnostic. To add support for a
new e-invoice format, extend `EInvoiceModel` and implement the abstract
hooks:

```java
public class MyFormatModel extends EInvoiceModel {

    public MyFormatModel(Document doc) {
        super(doc);
    }

    @Override
    public void setNameSpaces() {
        // register namespace URIs and prefixes
    }

    @Override
    public void parseContent() {
        // read your format's XML into the shared model fields
    }

    @Override
    protected void updateTradeTax() {
        // write totals and per-rate VAT breakdown into your XML
    }

    // Override setters (setId, setIssueDateTime, ...) to write into your XML
}
```

Then register your model in `EInvoiceModelFactory` by adding a branch that
detects your root element / namespace.

The `TradeLineItem` and `TradeParty` classes are plain POJOs - you can use
their fields directly in your own mapping code.

## API Notes and Gotchas

A few things that are easy to get wrong:

- **`setTotal()` means NET line total**, not gross. See the field reference
  above.
- **`setNetPrice()` must be set** in addition to `setGrossPrice()`. Many
  formats write both values; forgetting `netPrice` results in `0.0` in the
  generated XML.
- **Use `addTradeLineItem(...)`, not `setTradeLineItem(...)`.** The singular
  method does not exist. `addTradeLineItem` also handles fallback IDs,
  duplicate detection and total recalculation.
- **`setTaxRate(...)` - not `setVat(...)`.** The method on `TradeLineItem`
  is named `setTaxRate`.
- **Header totals are read-only** when building an invoice from line items.
  They are recalculated on every `addTradeLineItem(...)` call.
- **Format-specific differences:** e.g. KSeF FA(3) writes the net unit price
  (`P_9A`) from `netPrice`, while CII writes both `GrossPriceProductTradePrice`
  and `NetPriceProductTradePrice`. Consult the format documentation for the
  exact mapping.

## Validating an E-Invoice

You can use the
[Online eInvoice Validator](https://www.itb.ec.europa.eu/invoice/upload)
to test an e-invoice document.

## How to Join this Project

We maintain Imixs e-invoice as an open source project on GitHub and welcome
developers to join our community. Whether you want to fix bugs, add new
features, or improve documentation - your contributions are valuable to us.
You can start by forking the repository, creating issues for bug reports or
feature requests, or submitting pull requests with your improvements. We
actively review contributions and provide feedback to ensure high code
quality. We follow standard GitHub workflows and maintain our codebase under
the MIT license, making it easy for anyone to participate. If you're
interested in contributing, check out our GitHub repository and feel free to
reach out through issues or discussions.

We're particularly interested in contributions around:

- **UBL write support**
- **Validation features**
- **Performance optimizations**
- **Additional national formats** (building on the extensible model API)
