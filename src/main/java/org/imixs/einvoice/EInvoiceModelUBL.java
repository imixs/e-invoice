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

        Element accountingSupplierPartyElement = findChildNode(getRoot(), EInvoiceNS.CAC,
                "AccountingSupplierParty");
        if (accountingSupplierPartyElement != null) {
            getTradeParties().add(parseTradeParty(accountingSupplierPartyElement, "seller"));
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
     * 
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

            // Product details
            Element product = findChildNode(lineItem, EInvoiceNS.CAC, "Item");
            if (product != null) {
                Element nameElement = findChildNode(product, EInvoiceNS.CBC, "Name");
                if (nameElement != null) {
                    item.setName(nameElement.getTextContent());
                }

            }

            // Price info
            Element price = findChildNode(lineItem, EInvoiceNS.CAC, "Price");
            if (price != null) {
                // <cbc:PriceAmount currencyID="EUR">9.9</cbc:PriceAmount>
                Element priceAmount = findChildNode(price, EInvoiceNS.CBC, "PriceAmount");
                if (priceAmount != null) {
                    item.setGrossPrice(Double.parseDouble(priceAmount.getTextContent()));
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

            // Quantity

            Element quantity = findChildNode(lineItem, EInvoiceNS.CBC, "InvoicedQuantity");
            if (quantity != null) {
                item.setQuantity(Double.parseDouble(quantity.getTextContent()));
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

    @Override
    public void setNetTotalAmount(BigDecimal value) {
        super.setNetTotalAmount(value);
        Element monetaryTotalElement = findOrCreateChildNode(getRoot(), EInvoiceNS.CAC, "LegalMonetaryTotal");
        Element lineExtensionElement = findOrCreateChildNode(monetaryTotalElement, EInvoiceNS.CBC,
                "LineExtensionAmount");
        lineExtensionElement.setTextContent(value.toPlainString());
    }

    @Override
    public void setGrandTotalAmount(BigDecimal value) {
        super.setGrandTotalAmount(value);
        Element monetaryTotalElement = findOrCreateChildNode(getRoot(), EInvoiceNS.CAC, "LegalMonetaryTotal");
        Element taxInclusiveElement = findOrCreateChildNode(monetaryTotalElement, EInvoiceNS.CBC, "TaxInclusiveAmount");
        taxInclusiveElement.setTextContent(value.toPlainString());
    }

    @Override
    public void setTaxTotalAmount(BigDecimal value) {
        super.setTaxTotalAmount(value);
        Element taxTotalElement = findOrCreateChildNode(getRoot(), EInvoiceNS.CAC, "TaxTotal");
        Element taxAmountElement = findOrCreateChildNode(taxTotalElement, EInvoiceNS.CBC, "TaxAmount");
        taxAmountElement.setTextContent(value.toPlainString());
    }

}
