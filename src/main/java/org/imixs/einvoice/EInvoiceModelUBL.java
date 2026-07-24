package org.imixs.einvoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * A EInvoiceModel represents the dom tree of a e-invoice.
 * <p>
 * The model elements can be read only.
 * 
 * @author rsoika
 *
 */
public class EInvoiceModelUBL extends EInvoiceModel {

    public EInvoiceModelUBL(Document doc) {
        super(doc);
    }

    /**
     * This method instantiates a new BPMN model with the default BPMN namespaces
     * and prefixes.
     * 
     * @param doc
     */
    @Override
    public void setNameSpaces() {
        // Set default URIs and prefixes
        setUri(EInvoiceNS.CAC, "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
        setUri(EInvoiceNS.CBC, "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");

        // Set initial default prefixes
        setPrefix(EInvoiceNS.CAC, "cac");
        setPrefix(EInvoiceNS.CBC, "cbc");

        // Parse all namespaces from the root element
        NamedNodeMap attributes = getRoot().getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            String nodeName = node.getNodeName();
            String nodeValue = node.getNodeValue();

            // Handle both xmlns:prefix and xmlns declarations
            String prefix = null;
            if (nodeName.startsWith("xmlns:")) {
                prefix = nodeName.substring(6); // remove "xmlns:"
            } else if ("xmlns".equals(nodeName)) {
                prefix = ""; // default namespace
            }

            if (prefix != null) {
                // Match by namespace URI
                if (nodeValue.equals(getUri(EInvoiceNS.CAC))) {
                    logger.fine("...set CAC namespace prefix: " + prefix);
                    setPrefix(EInvoiceNS.CAC, prefix);
                } else if (nodeValue.equals(getUri(EInvoiceNS.CBC))) {
                    logger.fine("...set CBC namespace prefix: " + prefix);
                    setPrefix(EInvoiceNS.CBC, prefix);
                }
                // Optional: store unknown namespaces for future reference
                else {
                    logger.fine("Found additional namespace - prefix: " + prefix + ", URI: " + nodeValue);
                }
            }
        }

        // Validate that required namespaces were found
        if (getPrefix(EInvoiceNS.CAC) == null || getPrefix(EInvoiceNS.CBC) == null) {
            logger.warning("Required namespaces (CAC and/or CBC) not found in document!");
        }
    }

    /**
     * This method instantiates a new eInvoice model based on a given
     * org.w3c.dom.Document. The method parses the namespaces.
     * <p>
     * 
     * 
     * 
     */
    @Override
    public void parseContent() {
        // Load e-invoice standard data
        loadDocumentCoreData();

    }

    /**
     * This method parses the xml content and builds the model.
     * 
     */
    private void loadDocumentCoreData() {
        Element element = null;
        // cbc:ID
        element = findChildNode(getRoot(), EInvoiceNS.CBC, "ID");
        if (element != null) {
            setId(element.getTextContent());
        }

        // read Date time
        element = findChildNode(getRoot(), EInvoiceNS.CBC, "IssueDate");
        if (element != null) {
            String dateStr = element.getTextContent();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            setIssueDateTime(LocalDate.parse(dateStr, formatter));
        }

        // read currency (cbc:DocumentCurrencyCode)
        element = findChildNode(getRoot(), EInvoiceNS.CBC, "DocumentCurrencyCode");
        if (element != null && !element.getTextContent().isBlank()) {
            setCurrency(element.getTextContent());
        }

        Element accountingSupplierPartyElement = findChildNode(getRoot(), EInvoiceNS.CAC,
                "AccountingSupplierParty");
        if (accountingSupplierPartyElement != null) {
            getTradeParties().add(parseTradeParty(accountingSupplierPartyElement, "seller"));
        }

        Element accountingCustomerPartyElement = findChildNode(getRoot(), EInvoiceNS.CAC,
                "AccountingCustomerParty");
        if (accountingCustomerPartyElement != null) {
            getTradeParties().add(parseTradeParty(accountingCustomerPartyElement, "buyer"));
        }

        Element orderReferenceElement = findChildNode(getRoot(), EInvoiceNS.CAC,
                "OrderReference");
        if (orderReferenceElement != null) {
            Element _idElement = findChildNode(orderReferenceElement, EInvoiceNS.CBC, "ID");
            if (_idElement != null) {
                setOrderReferenceId(_idElement.getTextContent());
            }
        }

        parseTotal();

        // read line items...
        Set<TradeLineItem> lineItems = parseTradeLineItems();
        this.setTradeLineItems(lineItems);
    }

    /**
     * Parse monetary totals
     * <p>
     * Note: this reads the aggregated totals from the XML into the plain
     * model fields (via the base class setters, which no longer have any
     * XML side effects). This is only used when parsing an existing
     * document; when building/updating an invoice, the totals are always
     * derived from the line items via {@link #updateTradeTax()}.
     */
    public void parseTotal() {
        Element monetaryTotalElement = findChildNode(getRoot(), EInvoiceNS.CAC,
                "LegalMonetaryTotal");
        if (monetaryTotalElement != null) {

            Element child = null;
            child = findChildNode(monetaryTotalElement, EInvoiceNS.CBC,
                    "TaxInclusiveAmount");
            if (child != null) {
                setGrandTotalAmount(new BigDecimal(child.getTextContent()).setScale(2, RoundingMode.HALF_UP));
            }
            // net
            child = findChildNode(monetaryTotalElement, EInvoiceNS.CBC,
                    "LineExtensionAmount");
            if (child != null) {
                setNetTotalAmount(new BigDecimal(child.getTextContent()).setScale(2, RoundingMode.HALF_UP));
            }
            // tax
            setTaxTotalAmount(getGrandTotalAmount().subtract(getNetTotalAmount().setScale(2, RoundingMode.HALF_UP)));

        }

    }

    /**
     * Parse a TradeParty element
     * 
     * @param tradePartyElement
     * @param type
     * @return
     */
    public TradeParty parseTradeParty(Element tradePartyElement, String type) {
        TradeParty tradeParty = new TradeParty(type);
        Element partyElement = null;

        // Parse name
        partyElement = findChildNode(tradePartyElement, EInvoiceNS.CAC,
                "Party");
        if (partyElement != null) {
            // partyname
            Element element = findChildNode(partyElement, EInvoiceNS.CAC,
                    "PartyName");
            if (element != null) {
                element = findChildNode(element, EInvoiceNS.CBC,
                        "Name");
                if (element != null) {
                    tradeParty.setName(element.getTextContent());
                }
            }

        }

        return tradeParty;
    }

    /**
     * Parse the trade line items and return a list of items collected
     * 
     * <cac:InvoiceLine>
     * 
     * @return
     */
    public Set<TradeLineItem> parseTradeLineItems() {
        Set<TradeLineItem> items = new LinkedHashSet<>();

        Set<Element> lineItems = findChildNodesByName(getRoot(), EInvoiceNS.CAC,
                "InvoiceLine");

        for (Element lineItem : lineItems) {
            // Get Line ID

            Element idElement = findChildNode(lineItem, EInvoiceNS.CBC, "ID");
            if (idElement == null)
                continue;

            TradeLineItem item = new TradeLineItem(idElement.getTextContent());

            // Quantity
            Element quantity = findChildNode(lineItem, EInvoiceNS.CBC, "InvoicedQuantity");
            if (quantity != null) {
                item.setQuantity(Double.parseDouble(quantity.getTextContent()));
            }

            // LineExtensionAmount (net total of this line)
            Element lineExtension = findChildNode(lineItem, EInvoiceNS.CBC, "LineExtensionAmount");
            if (lineExtension != null) {
                item.setTotal(Double.parseDouble(lineExtension.getTextContent()));
            }

            // Product details
            Element product = findChildNode(lineItem, EInvoiceNS.CAC, "Item");
            if (product != null) {
                Element nameElement = findChildNode(product, EInvoiceNS.CBC, "Name");
                if (nameElement != null) {
                    item.setName(nameElement.getTextContent());
                }

                // Tax rate (cac:ClassifiedTaxCategory/cbc:Percent)
                Element taxCategory = findChildNode(product, EInvoiceNS.CAC, "ClassifiedTaxCategory");
                if (taxCategory != null) {
                    Element percentElement = findChildNode(taxCategory, EInvoiceNS.CBC, "Percent");
                    if (percentElement != null && !percentElement.getTextContent().isBlank()) {
                        item.setTaxRate(Double.parseDouble(percentElement.getTextContent()));
                    }
                }
            }

            // Price info
            Element price = findChildNode(lineItem, EInvoiceNS.CAC, "Price");
            if (price != null) {
                // <cbc:PriceAmount currencyID="EUR">9.9</cbc:PriceAmount>
                Element priceAmount = findChildNode(price, EInvoiceNS.CBC, "PriceAmount");
                if (priceAmount != null) {
                    item.setGrossPrice(Double.parseDouble(priceAmount.getTextContent()));
                    item.setNetPrice(Double.parseDouble(priceAmount.getTextContent()));
                }
            }

            // OrderLineReference
            Element orderLineReference = findChildNode(lineItem, EInvoiceNS.CAC, "OrderLineReference");
            if (orderLineReference != null) {
                // <cbc:PriceAmount currencyID="EUR">9.9</cbc:PriceAmount>
                Element lineID = findChildNode(orderLineReference, EInvoiceNS.CBC, "LineID");
                if (lineID != null) {
                    item.setOrderReferenceId(lineID.getTextContent());
                }
            }

            items.add(item);
        }

        return items;

    }

    @Override
    public void setId(String value) {
        super.setId(value);
        Element element = findOrCreateChildNode(getRoot(), EInvoiceNS.CBC, "ID");
        element.setTextContent(value);
    }

    @Override
    public void setIssueDateTime(LocalDate value) {
        super.setIssueDateTime(value);
        Element element = findOrCreateChildNode(getRoot(), EInvoiceNS.CBC, "IssueDate");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        element.setTextContent(formatter.format(value));
    }

    @Override
    public void setOrderReferenceId(String value) {
        super.setOrderReferenceId(value);
        Element orderrefElement = findOrCreateChildNode(getRoot(), EInvoiceNS.CAC, "OrderReference");
        Element element = findOrCreateChildNode(orderrefElement, EInvoiceNS.CBC, "ID");
        element.setTextContent(value);
    }

    @Override
    public void setDueDateTime(LocalDate value) {
        super.setDueDateTime(value);
        Element element = findOrCreateChildNode(getRoot(), EInvoiceNS.CBC, "DueDate");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        element.setTextContent(formatter.format(value));
    }

    /**
     * Update the invoice currency code (cbc:DocumentCurrencyCode)
     */
    @Override
    public void setCurrency(String value) {
        super.setCurrency(value);
        updateElementValue(getRoot(), EInvoiceNS.CBC, "DocumentCurrencyCode", value);
    }

    /**
     * Adds a new TradeLineItem into the XML tree (cac:InvoiceLine).
     * <p>
     * Per UBL schema sequence, InvoiceLine elements are the LAST children
     * of the Invoice root element - they must appear after TaxTotal and
     * LegalMonetaryTotal. Since {@link #updateTradeTax()} (triggered by
     * {@code super.addTradeLineItem(item)} below) creates/updates those
     * elements if they don't exist yet, and always keeps them positioned
     * before the first existing InvoiceLine, appending new InvoiceLine
     * elements at the end of the root is always schema-correct.
     * 
     * @param item
     */
    @Override
    public void addTradeLineItem(TradeLineItem item) {
        if (item == null) {
            return;
        }

        // Recalculates totals and calls updateTradeTax() BEFORE we append
        // the new InvoiceLine element below, so TaxTotal/LegalMonetaryTotal
        // are always created/positioned before any InvoiceLine.
        super.addTradeLineItem(item);

        // Remove a previously written InvoiceLine element with the same ID,
        // if present, so the XML stays in sync when the same position is
        // added more than once.
        removeExistingLineItemElement(item.getId());

        Element lineItem = createChildNode(getRoot(), EInvoiceNS.CAC, "InvoiceLine");

        // cbc:ID - line number
        updateElementValue(lineItem, EInvoiceNS.CBC, "ID", item.getId());

        // cbc:InvoicedQuantity
        Element quantityElement = createChildNode(lineItem, EInvoiceNS.CBC, "InvoicedQuantity");
        quantityElement.setAttribute("unitCode", "C62");
        quantityElement.setTextContent(String.valueOf(item.getQuantity()));

        // cbc:LineExtensionAmount - net total of this line
        Element lineExtensionElement = createChildNode(lineItem, EInvoiceNS.CBC, "LineExtensionAmount");
        lineExtensionElement.setAttribute("currencyID", getCurrency());
        lineExtensionElement.setTextContent(
                BigDecimal.valueOf(item.getTotal()).setScale(2, RoundingMode.HALF_UP).toPlainString());

        // cac:Item
        Element itemElement = createChildNode(lineItem, EInvoiceNS.CAC, "Item");
        if (item.getName() != null) {
            updateElementValue(itemElement, EInvoiceNS.CBC, "Name", item.getName());
        }
        if (item.getDescription() != null) {
            updateElementValue(itemElement, EInvoiceNS.CBC, "Description", item.getDescription());
        }

        // cac:ClassifiedTaxCategory
        Element taxCategoryElement = createChildNode(itemElement, EInvoiceNS.CAC, "ClassifiedTaxCategory");
        updateElementValue(taxCategoryElement, EInvoiceNS.CBC, "ID", item.getTaxRate() > 0 ? "S" : "Z");
        updateElementValue(taxCategoryElement, EInvoiceNS.CBC, "Percent",
                BigDecimal.valueOf(item.getTaxRate()).toPlainString());
        Element taxSchemeElement = createChildNode(taxCategoryElement, EInvoiceNS.CAC, "TaxScheme");
        updateElementValue(taxSchemeElement, EInvoiceNS.CBC, "ID", "VAT");

        // order ref id - cac:OrderLineReference/cbc:LineID
        if (item.getOrderReferenceId() != null && !item.getOrderReferenceId().isEmpty()) {
            Element orderLineReference = createChildNode(lineItem, EInvoiceNS.CAC, "OrderLineReference");
            updateElementValue(orderLineReference, EInvoiceNS.CBC, "LineID", item.getOrderReferenceId());
        }

        // cac:Price
        Element priceElement = createChildNode(lineItem, EInvoiceNS.CAC, "Price");
        Element priceAmountElement = createChildNode(priceElement, EInvoiceNS.CBC, "PriceAmount");
        priceAmountElement.setAttribute("currencyID", getCurrency());
        priceAmountElement.setTextContent(
                BigDecimal.valueOf(item.getNetPrice()).setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    /**
     * Removes an existing {@code cac:InvoiceLine} XML element that has the
     * given line ID, if present.
     * <p>
     * Used by {@link #addTradeLineItem(TradeLineItem)} to keep the XML in
     * sync with the model when a line item with the same ID is added
     * more than once.
     */
    private void removeExistingLineItemElement(String lineId) {
        if (lineId == null || lineId.isEmpty()) {
            return;
        }
        Set<Element> existingLineItems = findChildNodesByName(getRoot(), EInvoiceNS.CAC, "InvoiceLine");
        for (Element existing : existingLineItems) {
            Element idElement = findChildNode(existing, EInvoiceNS.CBC, "ID");
            if (idElement != null && lineId.equals(idElement.getTextContent())) {
                getRoot().removeChild(existing);
                break; // line ID is unique, no need to keep searching
            }
        }
    }

    /**
     * Clears all trade line items from the model AND removes all
     * {@code cac:InvoiceLine} XML elements from the DOM, so model and XML
     * stay in sync when an invoice is rebuilt from scratch (e.g.
     * re-processing the same template a second time).
     */
    @Override
    public void resetTradeLineItems() {
        super.resetTradeLineItems();
        Set<Element> existingLineItems = findChildNodesByName(getRoot(), EInvoiceNS.CAC, "InvoiceLine");
        for (Element existing : existingLineItems) {
            getRoot().removeChild(existing);
        }
    }

    /**
     * Writes the recalculated totals and the per-rate VAT breakdown into the
     * XML structure (cac:TaxTotal / cac:TaxSubtotal and
     * cac:LegalMonetaryTotal).
     * <p>
     * Called automatically by {@link EInvoiceModel#recalculateTotals()}
     * whenever a line item is added. Removes the existing {@code TaxTotal}
     * block on root level and re-creates it with one {@code TaxSubtotal}
     * per distinct tax rate (mirrors BG-23 VAT BREAKDOWN in CII/EN16931),
     * so invoices with mixed tax rates are represented correctly.
     * <p>
     * Per UBL schema sequence, {@code TaxTotal} and {@code LegalMonetaryTotal}
     * must appear before any {@code InvoiceLine} element. If InvoiceLine
     * elements already exist (from previously added line items), the new
     * elements are inserted before the first of them; otherwise they are
     * simply appended at the end of the root.
     */
    @Override
    protected void updateTradeTax() {

        Element insertBeforeElement = getFirstInvoiceLineElement();

        // --- TaxTotal / TaxSubtotal -------------------------------------
        Element existingTaxTotal = findChildNode(getRoot(), EInvoiceNS.CAC, "TaxTotal");
        if (existingTaxTotal != null) {
            getRoot().removeChild(existingTaxTotal);
        }

        Element taxTotalElement = createChildNode(getRoot(), EInvoiceNS.CAC, "TaxTotal", insertBeforeElement);

        BigDecimal tax = getTaxTotalAmount();
        Element taxAmountElement = createChildNode(taxTotalElement, EInvoiceNS.CBC, "TaxAmount");
        taxAmountElement.setAttribute("currencyID", getCurrency());
        taxAmountElement.setTextContent(tax.toPlainString());

        for (TaxBreakdown breakdown : getTaxBreakdown()) {
            Element taxSubtotal = createChildNode(taxTotalElement, EInvoiceNS.CAC, "TaxSubtotal");

            Element taxableAmountElement = createChildNode(taxSubtotal, EInvoiceNS.CBC, "TaxableAmount");
            taxableAmountElement.setAttribute("currencyID", getCurrency());
            taxableAmountElement.setTextContent(breakdown.getBasisAmount().toPlainString());

            Element subtotalTaxAmountElement = createChildNode(taxSubtotal, EInvoiceNS.CBC, "TaxAmount");
            subtotalTaxAmountElement.setAttribute("currencyID", getCurrency());
            subtotalTaxAmountElement.setTextContent(breakdown.getTaxAmount().toPlainString());

            Element taxCategoryElement = createChildNode(taxSubtotal, EInvoiceNS.CAC, "TaxCategory");
            updateElementValue(taxCategoryElement, EInvoiceNS.CBC, "ID",
                    breakdown.getRate().doubleValue() > 0 ? "S" : "Z");
            updateElementValue(taxCategoryElement, EInvoiceNS.CBC, "Percent", breakdown.getRate().toPlainString());
            Element taxSchemeElement = createChildNode(taxCategoryElement, EInvoiceNS.CAC, "TaxScheme");
            updateElementValue(taxSchemeElement, EInvoiceNS.CBC, "ID", "VAT");
        }

        // --- LegalMonetaryTotal ------------------------------------------
        BigDecimal net = getNetTotalAmount();
        BigDecimal grand = getGrandTotalAmount();

        Element monetaryTotalElement = findChildNode(getRoot(), EInvoiceNS.CAC, "LegalMonetaryTotal");
        if (monetaryTotalElement == null) {
            // (re-)determine insertion point in case TaxTotal above was
            // just newly created and is now the element to insert after
            monetaryTotalElement = createChildNode(getRoot(), EInvoiceNS.CAC, "LegalMonetaryTotal",
                    getFirstInvoiceLineElement());
        }

        Element lineExtensionElement = updateElementValue(monetaryTotalElement, EInvoiceNS.CBC,
                "LineExtensionAmount", net.toPlainString());
        if (lineExtensionElement != null) {
            lineExtensionElement.setAttribute("currencyID", getCurrency());
        }
        Element taxExclusiveElement = updateElementValue(monetaryTotalElement, EInvoiceNS.CBC,
                "TaxExclusiveAmount", net.toPlainString());
        if (taxExclusiveElement != null) {
            taxExclusiveElement.setAttribute("currencyID", getCurrency());
        }
        Element taxInclusiveElement = updateElementValue(monetaryTotalElement, EInvoiceNS.CBC,
                "TaxInclusiveAmount", grand.toPlainString());
        if (taxInclusiveElement != null) {
            taxInclusiveElement.setAttribute("currencyID", getCurrency());
        }
        Element payableAmountElement = updateElementValue(monetaryTotalElement, EInvoiceNS.CBC,
                "PayableAmount", grand.toPlainString());
        if (payableAmountElement != null) {
            payableAmountElement.setAttribute("currencyID", getCurrency());
        }
    }

    /**
     * Returns the first existing {@code cac:InvoiceLine} child element of
     * the root, or {@code null} if none exists yet. Used to keep
     * {@code TaxTotal}/{@code LegalMonetaryTotal} correctly positioned
     * before all invoice lines per the UBL schema sequence.
     */
    private Element getFirstInvoiceLineElement() {
        String prefix = getPrefix(EInvoiceNS.CAC);
        String tagName = prefix + "InvoiceLine";
        Node child = getRoot().getFirstChild();
        while (child != null) {
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                return (Element) child;
            }
            child = child.getNextSibling();
        }
        return null;
    }

}