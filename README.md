# e-invoice

Imixs e-invoice is a simple Java library to read and write e-invoice documents. The advantage of this library is that no dependencies to any other non-java standard package is needed. So this library can be integrated into any modern Java project to read and write a e-invoice file. 

The project supports `factur-x` (CII) and the `UBL` format. 

**Note:** A this moment the library supports read and write for factur-x invoices. UBL can only be read. 

## How to use: 

To add this libray to your Maven project just add the following maven dependency:

```xml
    <dependency>
        <groupId>org.imixs.util</groupId>
        <artifactId>imixs-e-invoice</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
```

## Examples

### Parse & Read an E-Invoice document

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