package org.imixs.einvoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
            id = element.getTextContent();
        }

        // read Date time
        element = findChildNode(getRoot(), EInvoiceNS.CBC, "IssueDate");
        if (element != null) {
            String dateStr = element.getTextContent();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            issueDateTime = LocalDate.parse(dateStr, formatter);
        }

        Element accountingSupplierPartyElement = findChildNode(getRoot(), EInvoiceNS.CAC,
                "AccountingSupplierParty");
        if (accountingSupplierPartyElement != null) {
            tradeParties.add(parseTradeParty(accountingSupplierPartyElement, "seller"));
        }

        parseTotal();

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
                grandTotalAmount = new BigDecimal(child.getTextContent()).setScale(2, RoundingMode.HALF_UP);
            }
            // net
            child = findChildNode(monetaryTotalElement, EInvoiceNS.CBC,
                    "LineExtensionAmount");
            if (child != null) {
                netTotalAmount = new BigDecimal(child.getTextContent()).setScale(2, RoundingMode.HALF_UP);
            }
            // tax
            taxTotalAmount = grandTotalAmount.subtract(netTotalAmount).setScale(2, RoundingMode.HALF_UP);

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

    @Override
    public void setId(String value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setId'");
    }

    @Override
    public void setIssueDateTime(LocalDate value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setIssueDateTime'");
    }

    @Override
    public void setNetTotalAmount(BigDecimal value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setNetTotalAmount'");
    }

    @Override
    public void setGrandTotalAmount(BigDecimal value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setGrandTotalAmount'");
    }

    @Override
    public void setDueDateTime(LocalDate value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDueDateTime'");
    }

    @Override
    public void setTaxTotalAmount(BigDecimal value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setTaxTotalAmount'");
    }

}
