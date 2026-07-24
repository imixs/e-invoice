package org.imixs.einvoice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests the WRITE path of EInvoiceModel: addTradeLineItem(...) and the
 * automatic recalculation of totals / tax breakdown via updateTradeTax().
 * <p>
 * Since {@link EInvoiceModelFactory} can only read an existing document
 * (there is no "create empty model" factory method), each test loads an
 * existing template resource, clears the parsed line items via
 * {@link EInvoiceModel#resetTradeLineItems()}, and then rebuilds the
 * invoice from scratch using addTradeLineItem(...). This exercises exactly
 * the same code path the adapters use when generating a new invoice.
 */
class EInvoiceModelWriteTest {

    private EInvoiceModel loadTemplate(String resourcePath) throws IOException, EInvoiceFormatException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return EInvoiceModelFactory.read(is);
        }
    }

    // ------------------------------------------------------------------
    // CII - single tax rate
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CII: addTradeLineItem recalculates totals for a single tax rate")
    void testAddTradeLineItemCIISingleRate() throws Exception {
        EInvoiceModel model = loadTemplate("e-invoice/Rechnung_R_00010.xml");
        assertNotNull(model);
        assertTrue(model instanceof EInvoiceModelCII);

        // Start from a clean slate - discard whatever line items were parsed
        model.resetTradeLineItems();

        TradeLineItem item1 = new TradeLineItem("1");
        item1.setName("Consulting");
        item1.setNetPrice(1000.00);
        item1.setGrossPrice(1000.00);
        item1.setQuantity(1);
        item1.setTaxRate(19.0);
        item1.setTotal(1000.00);
        model.addTradeLineItem(item1);

        TradeLineItem item2 = new TradeLineItem("2");
        item2.setName("Travel expenses");
        item2.setNetPrice(500.00);
        item2.setGrossPrice(500.00);
        item2.setQuantity(1);
        item2.setTaxRate(19.0);
        item2.setTotal(500.00);
        model.addTradeLineItem(item2);

        // Totals must reflect both positions
        assertEquals(new BigDecimal("1500.00"), model.getNetTotalAmount());
        assertEquals(new BigDecimal("285.00"), model.getTaxTotalAmount());
        assertEquals(new BigDecimal("1785.00"), model.getGrandTotalAmount());

        // Only one distinct tax rate -> exactly one breakdown entry
        assertEquals(1, model.getTaxBreakdown().size());
        EInvoiceModel.TaxBreakdown breakdown = model.getTaxBreakdown().iterator().next();
        assertEquals(0, breakdown.getRate().compareTo(new BigDecimal("19.0")));
        assertEquals(0, breakdown.getBasisAmount().compareTo(new BigDecimal("1500.00")), "expected 1500.00");
        assertEquals(0, breakdown.getTaxAmount().compareTo(new BigDecimal("285.00")), "expected 285.00");

        // The generated XML must contain exactly two line items and one
        // ApplicableTradeTax block on header level (single rate)
        String xml = new String(model.getContent(), StandardCharsets.UTF_8);
        assertEquals(2, countOccurrences(xml, "<ram:AssociatedDocumentLineDocument>"));
    }

    // ------------------------------------------------------------------
    // CII - mixed tax rates
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CII: addTradeLineItem correctly separates mixed tax rates")
    void testAddTradeLineItemCIIMixedRates() throws Exception {
        EInvoiceModel model = loadTemplate("e-invoice/Rechnung_R_00010.xml");
        model.resetTradeLineItems();

        TradeLineItem item1 = new TradeLineItem("1");
        item1.setName("Export service (0%)");
        item1.setNetPrice(14040.00);
        item1.setGrossPrice(14040.00);
        item1.setQuantity(1);
        item1.setTaxRate(0.0);
        item1.setTotal(14040.00);
        model.addTradeLineItem(item1);

        TradeLineItem item2 = new TradeLineItem("2");
        item2.setName("Domestic service (23%)");
        item2.setNetPrice(3960.00);
        item2.setGrossPrice(3960.00);
        item2.setQuantity(1);
        item2.setTaxRate(23.0);
        item2.setTotal(3960.00);
        model.addTradeLineItem(item2);

        // Two distinct rates -> two breakdown entries
        assertEquals(2, model.getTaxBreakdown().size());

        assertEquals(new BigDecimal("18000.00"), model.getNetTotalAmount());
        assertEquals(new BigDecimal("910.80"), model.getTaxTotalAmount());
        assertEquals(new BigDecimal("18910.80"), model.getGrandTotalAmount());

        boolean foundZero = false;
        boolean found23 = false;
        for (EInvoiceModel.TaxBreakdown breakdown : model.getTaxBreakdown()) {
            if (breakdown.getRate().doubleValue() == 0.0) {
                foundZero = true;
                assertEquals(0, breakdown.getBasisAmount().compareTo(new BigDecimal("14040.00")), "expected 14040.00");
                assertEquals(0, breakdown.getTaxAmount().compareTo(new BigDecimal("0.00")), "expected 0.00");
            } else if (breakdown.getRate().doubleValue() == 23.0) {
                found23 = true;
                assertEquals(0, breakdown.getBasisAmount().compareTo(new BigDecimal("3960.00")), "expected 3960.00");
                assertEquals(0, breakdown.getTaxAmount().compareTo(new BigDecimal("910.80")), "expected 910.80");
            }
        }
        assertTrue(foundZero, "Expected a 0% tax breakdown entry");
        assertTrue(found23, "Expected a 23% tax breakdown entry");

        // The generated XML must contain two separate ApplicableTradeTax
        // blocks on header level (one per distinct rate), positioned
        // BEFORE SpecifiedTradePaymentTerms / the monetary summation.
        String xml = new String(model.getContent(), StandardCharsets.UTF_8);
        int taxBlocksOnHeaderLevel = countOccurrences(xml, "<ram:CalculatedAmount>");
        // 2 header-level ApplicableTradeTax blocks (line items in this
        // template do not carry CalculatedAmount, only header does)
        assertEquals(2, taxBlocksOnHeaderLevel);

        int paymentTermsIndex = xml.indexOf("SpecifiedTradePaymentTerms");
        int lastTaxBlockIndex = xml.lastIndexOf("<ram:CalculatedAmount>");
        assertTrue(lastTaxBlockIndex < paymentTermsIndex,
                "ApplicableTradeTax must be positioned before SpecifiedTradePaymentTerms");
    }

    // ------------------------------------------------------------------
    // CII - duplicate line item id must not create a duplicate XML block
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CII: adding the same line item id twice does not duplicate the XML block")
    void testAddTradeLineItemCIIDuplicateId() throws Exception {
        EInvoiceModel model = loadTemplate("e-invoice/Rechnung_R_00010.xml");
        model.resetTradeLineItems();

        TradeLineItem item = new TradeLineItem("1");
        item.setName("First version");
        item.setNetPrice(100.00);
        item.setGrossPrice(100.00);
        item.setQuantity(1);
        item.setTaxRate(19.0);
        item.setTotal(100.00);
        model.addTradeLineItem(item);

        // Add again with the SAME id but different data - simulates a
        // re-processing of the same position.
        TradeLineItem updatedItem = new TradeLineItem("1");
        updatedItem.setName("Corrected version");
        updatedItem.setNetPrice(150.00);
        updatedItem.setGrossPrice(150.00);
        updatedItem.setQuantity(1);
        updatedItem.setTaxRate(19.0);
        updatedItem.setTotal(150.00);
        model.addTradeLineItem(updatedItem);

        // Model must only contain one line item
        assertEquals(1, model.getTradeLineItems().size());
        assertEquals(new BigDecimal("150.00"), model.getNetTotalAmount());

        // XML must only contain ONE AssociatedDocumentLineDocument block,
        // with the corrected data, not two.
        String xml = new String(model.getContent(), StandardCharsets.UTF_8);
        assertEquals(1, countOccurrences(xml, "<ram:AssociatedDocumentLineDocument>"));
        assertTrue(xml.contains("Corrected version"));
        assertTrue(!xml.contains("First version"));
    }

    // ------------------------------------------------------------------
    // UBL - mixed tax rates
    // ------------------------------------------------------------------

    @Test
    @DisplayName("UBL: addTradeLineItem correctly separates mixed tax rates")
    void testAddTradeLineItemUBLMixedRates() throws Exception {
        EInvoiceModel model = loadTemplate("e-invoice/EN16931_Einfach.ubl.xml");
        assertTrue(model instanceof EInvoiceModelUBL);

        model.resetTradeLineItems();

        TradeLineItem item1 = new TradeLineItem("1");
        item1.setName("Goods (7%)");
        item1.setNetPrice(200.00);
        item1.setGrossPrice(200.00);
        item1.setQuantity(1);
        item1.setTaxRate(7.0);
        item1.setTotal(200.00);
        model.addTradeLineItem(item1);

        TradeLineItem item2 = new TradeLineItem("2");
        item2.setName("Service (19%)");
        item2.setNetPrice(300.00);
        item2.setGrossPrice(300.00);
        item2.setQuantity(1);
        item2.setTaxRate(19.0);
        item2.setTotal(300.00);
        model.addTradeLineItem(item2);

        assertEquals(2, model.getTaxBreakdown().size());
        assertEquals(new BigDecimal("500.00"), model.getNetTotalAmount());
        // 200 * 7% = 14.00, 300 * 19% = 57.00 -> 71.00
        assertEquals(new BigDecimal("71.00"), model.getTaxTotalAmount());
        assertEquals(new BigDecimal("571.00"), model.getGrandTotalAmount());

        // The generated XML must contain two TaxSubtotal blocks and both
        // InvoiceLine elements, with TaxTotal/LegalMonetaryTotal appearing
        // BEFORE the InvoiceLine elements per the UBL schema sequence.
        String xml = new String(model.getContent(), StandardCharsets.UTF_8);
        assertEquals(2, countOccurrences(xml, "TaxSubtotal>") / 2); // open+close tags
        assertEquals(2, countOccurrences(xml, "InvoiceLine>") / 2); // open+close tags

        int firstInvoiceLineIndex = xml.indexOf("InvoiceLine>");
        int legalMonetaryTotalIndex = xml.indexOf("LegalMonetaryTotal>");
        assertTrue(legalMonetaryTotalIndex < firstInvoiceLineIndex,
                "LegalMonetaryTotal must be positioned before InvoiceLine elements");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private int countOccurrences(String text, String pattern) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(pattern, idx)) != -1) {
            count++;
            idx += pattern.length();
        }
        return count;
    }
}