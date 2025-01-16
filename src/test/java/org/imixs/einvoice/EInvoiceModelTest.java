package org.imixs.einvoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * This test class is testing the EInvoiceModel and tests different
 * kind of files
 * 
 */
class EInvoiceModelTest {

    @BeforeEach
    public void setUp() {

    }

    @Test
    void testStandaloneXML() throws IOException {
        // Prepare test data
        EInvoiceModel eInvoiceModel = null;
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("e-invoice/Rechnung_R_00010.xml")) {
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

        assertEquals(new BigDecimal("4380.9"), eInvoiceModel.getGrandTotalAmount());
        assertEquals(new BigDecimal("510.9"), eInvoiceModel.getTaxTotalAmount());
        assertEquals(new BigDecimal("3870.00"), eInvoiceModel.getNetTotalAmount());

        // Test SellerTradeParty
        TradeParty seller = eInvoiceModel.findTradeParty("seller");
        assertNotNull(seller);
        assertEquals("Max Mustermann", seller.getName());
        assertEquals("DE111111111", seller.getVatNumber());

        TradeParty buyer = eInvoiceModel.findTradeParty("buyer");
        assertNotNull(buyer);
        assertEquals("Viborg Metall GbR", buyer.getName());

    }

    @Test
    void testStandaloneXMLUML() throws IOException {
        // Prepare test data
        EInvoiceModel eInvoiceModel = null;
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("e-invoice/EN16931_Einfach.ubl.xml")) {
            if (is == null) {
                throw new IOException("Resource not found");
            }
            eInvoiceModel = EInvoiceModelFactory.read(is);
        }

        // Verify the result
        assertNotNull(eInvoiceModel);
        assertEquals("471102", eInvoiceModel.getId());

        LocalDate invoiceDate = eInvoiceModel.getIssueDateTime();
        assertEquals(LocalDate.of(2018, 3, 5), invoiceDate);

        assertEquals(new BigDecimal("529.87"), eInvoiceModel.getGrandTotalAmount());
        assertEquals(new BigDecimal("56.87"), eInvoiceModel.getTaxTotalAmount());
        assertEquals(new BigDecimal("473.00"), eInvoiceModel.getNetTotalAmount());

        // Test SellerTradeParty
        TradeParty seller = eInvoiceModel.findTradeParty("seller");
        assertNotNull(seller);
        assertEquals("Lieferant GmbH", seller.getName());

    }

    @Test
    void testStandaloneXMLCustomNameSpace() throws IOException {
        // Prepare test data
        EInvoiceModel eInvoiceModel = null;
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream is = classLoader.getResourceAsStream("e-invoice/Rechnung_CustomNamespace_CII.xml")) {
            if (is == null) {
                throw new IOException("Resource not found");
            }
            eInvoiceModel = EInvoiceModelFactory.read(is);
        }

        // Verify the result
        assertNotNull(eInvoiceModel);
        assertEquals("102026", eInvoiceModel.getId());

        LocalDate invoiceDate = eInvoiceModel.getIssueDateTime();
        assertEquals(LocalDate.of(2025, 1, 1), invoiceDate);

        assertEquals(new BigDecimal("52.36"), eInvoiceModel.getGrandTotalAmount());

        // Test SellerTradeParty
        TradeParty seller = eInvoiceModel.findTradeParty("seller");
        assertNotNull(seller);
        assertEquals("Foo Innovation GmbH", seller.getName());

        TradeParty buyer = eInvoiceModel.findTradeParty("buyer");
        assertNotNull(buyer);
        assertEquals("ABC GmbH", buyer.getName());

    }

}