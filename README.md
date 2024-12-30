# e-invoice

Imixs e-invoice is a lightweight and efficient Java library for processing e-invoices documents. The library stands out for its independence from external dependencies and seamlessly integrates into modern Java projects. It supports the major European e-invoice standards factur-x (CII) and UBL.


## Key Features

 - Native Java support, integration without external dependencies
 - Support for e-invoice formats factur-x and UBL
 - Simple and intuitive API for developers
 - Open Source under MIT license

## Use Cases

The Imixs e-invoice library serves as a simple to use solution for parsing and writing XML e-invoice documents. 
You can easily leverage the library for building automated invoice processing systems and implementing export functions for standardized electronic invoices. Its support for multiple e-invoice formats makes it easy to use for projects involving automated processing and format migration, helping businesses adapt to different trading partner requirements.

## Technical Details

The library focuses on XML processing of e-invoices. For ZUGFERD-compliant PDF documents, the generated XML must be manually embedded into the PDF file. Currently, reading and writing of factur-x invoices is fully supported, while UBL is only available in read mode.

## How to use: 

Integration into Maven projects is done by adding the following dependency:

```xml
    <dependency>
        <groupId>org.imixs.util</groupId>
        <artifactId>imixs-e-invoice</artifactId>
        <version>1.0.0</version>
    </dependency>
```

## Examples

### Reading and Parsing an E-Invoice

To read a XML File from an InputStream and parse it with the Imixs e-invoice library:

```java
    EInvoiceModel eInvoiceModel =null;
    try (InputStream is = myInputStream) {
        if (is == null) {
            throw new IOException("Resource not found" );
        }
        eInvoiceModel= EInvoiceModelFactory.read(is);
    }

    
    // Verify the result
    assertNotNull(eInvoiceModel);
    assertEquals("R-00010", eInvoiceModel.getId());

    LocalDate invoiceDate = eInvoiceModel.getIssueDateTime();
    assertEquals(LocalDate.of(2021, 7, 28), invoiceDate);

    assertEquals(new BigDecimal("4380.9"), eInvoiceModel.getGrandTotalAmount());
    assertEquals(new BigDecimal("510.9"), eInvoiceModel.getTaxTotalAmount());
    assertEquals(new BigDecimal("3870.00"), eInvoiceModel.getNetTotalAmount());

    // Test SellerTradeParty
    TradeParty seller = eInvoiceModel.findTradeParty("seller");
    assertNotNull(seller);
    assertEquals("Max Mustermann", seller.getName());
    assertEquals("DE111111111", seller.getVatNumber());

```

You can find the full example in the JUnit test package of this project. 

## Create an E-Invoice Document

To create a new e-invoice document you can work with any valid e-invoice template as an XML file. The template is the base for the core model that can be updated by the library. See the following example code:

```java
    // read the XML Template
    EInvoiceModel model = EInvoiceModelFactory.read(new ByteArrayInputStream(myXMLTemplate));

    // Update data
    model.setNetTotalAmount(100.00);
    model.setTaxTotalAmount(19.00);
    model.setGrandTotalAmount(119.00);

    model.setIssueDateTime(myInvoiceDate);
    model.setId("R-10000");

    // Addresses
    TradeParty billingAddress = new TradeParty("buyer");
    billingAddress.setName("Max Mustermann");
    billingAddress.setPostcodeCode("1000");
    billingAddress.setCityName("Berlin");
    billingAddress.setStreetAddress("Lindenstr. 1");
    model.setTradeParty(billingAddress);

    
    // Invoice Items
    
    TradeLineItem tradeLineItem = new TradeLineItem("1");
    tradeLineItem.setName("Moon Rocket");
    tradeLineItem.setDescription("Fly my to the moon");
    tradeLineItem.setGrossPrice(1000000.00);
    tradeLineItem.setQuantity(1.0);
    tradeLineItem.setVat(19.0);
    tradeLineItem.setTotal(1000000.00);
    
    model.setTradeLineItem(tradeLineItem);
    

    // finally update the template file
    byte[] myEInvoice=model.getContent();

```


You can use the [Online eInvoice Validator](https://www.itb.ec.europa.eu/invoice/upload) to test a e-invoice document. 



## How to Join this Project

We maintain Imixs e-invoice as an open source project on GitHub and welcome developers to join our community. Whether you want to fix bugs, add new features, or improve documentation - your contributions are valuable to us. You can start by forking the repository, creating issues for bug reports or feature requests, or submitting pull requests with your improvements. We actively review contributions and provides feedback to ensure high code quality. We follow standard GitHub workflows and maintain our codebase under the MIT license, making it easy for anyone to participate. If you're interested in contributing, check out our GitHub repository and feel free to reach out through issues or discussions. 

We're particularly interested in contributions around UBL support, validation features, and performance optimizations.