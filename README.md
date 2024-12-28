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

To read a XML File from an InputStream and parse it with the Imixs e-invoice library:

```
        EInvoiceModel eInvoiceModel =null;
        ClassLoader classLoader = getClass().getClassLoader();
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

