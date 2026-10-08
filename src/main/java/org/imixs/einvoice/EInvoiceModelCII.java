package org.imixs.einvoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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
public class EInvoiceModelCII extends EInvoiceModel {

    protected Element exchangedDocumentContext;
    protected Element exchangedDocument;
    protected Element supplyChainTradeTransaction;
    protected Element applicableHeaderTradeSettlement;
    protected Element specifiedTradeSettlementHeaderMonetarySummation;
    protected Element applicableHeaderTradeDelivery;
    protected Element applicableHeaderTradeAgreement;

    public EInvoiceModelCII(Document doc) {
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
        // Standard namespaces
        setUri(EInvoiceNS.RSM, "urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100");
        setUri(EInvoiceNS.QDT, "urn:un:unece:uncefact:data:standard:QualifiedDataType:10");
        setUri(EInvoiceNS.RAM, "urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100");
        setUri(EInvoiceNS.UDT, "urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100");

        // Standard prefixes
        setPrefix(EInvoiceNS.RSM, "rsm");
        setPrefix(EInvoiceNS.QDT, "qdt");
        setPrefix(EInvoiceNS.RAM, "ram");
        setPrefix(EInvoiceNS.UDT, "udt");

        // Parse the namespaces from root element
        NamedNodeMap defAttributes = getRoot().getAttributes();
        for (int j = 0; j < defAttributes.getLength(); j++) {
            Node node = defAttributes.item(j);
            String localName = node.getLocalName();
            String nodeValue = node.getNodeValue();

            // Check for alternative namespace prefixes (mgns1, mgns2, mgns3)
            if (nodeValue.equals(getUri(EInvoiceNS.RSM))) {
                setPrefix(EInvoiceNS.RSM, localName.replace("xmlns:", ""));
            } else if (nodeValue.equals(getUri(EInvoiceNS.RAM))) {
                setPrefix(EInvoiceNS.RAM, localName.replace("xmlns:", ""));
            } else if (nodeValue.equals(getUri(EInvoiceNS.UDT))) {
                setPrefix(EInvoiceNS.UDT, localName.replace("xmlns:", ""));
            }
        }
    }

    /**
     * This method parses the xml content and builds the model.
     * 
     */
    @Override
    public void parseContent() {

        // Parse standard tags...
        exchangedDocumentContext = findOrCreateChildNode(getRoot(), EInvoiceNS.RSM,
                "ExchangedDocumentContext");
        exchangedDocument = findOrCreateChildNode(getRoot(), EInvoiceNS.RSM, "ExchangedDocument");
        supplyChainTradeTransaction = findOrCreateChildNode(getRoot(), EInvoiceNS.RSM,
                "SupplyChainTradeTransaction");

        applicableHeaderTradeAgreement = findOrCreateChildNode(supplyChainTradeTransaction, EInvoiceNS.RAM,
                "ApplicableHeaderTradeAgreement");
        applicableHeaderTradeDelivery = findOrCreateChildNode(supplyChainTradeTransaction, EInvoiceNS.RAM,
                "ApplicableHeaderTradeDelivery");

        applicableHeaderTradeSettlement = findOrCreateChildNode(supplyChainTradeTransaction, EInvoiceNS.RAM,
                "ApplicableHeaderTradeSettlement");
        specifiedTradeSettlementHeaderMonetarySummation = findChildNode(applicableHeaderTradeSettlement,
                EInvoiceNS.RAM,
                "SpecifiedTradeSettlementHeaderMonetarySummation");

        // Load e-invoice standard data
        loadDocumentCoreData();

    }

    private void loadDocumentCoreData() {
        Element element = null;

        // read invoice number
        element = findChildNode(exchangedDocument, EInvoiceNS.RAM, "ID");
        if (element != null) {
            setId(element.getTextContent());
        }

        // read invoice type code
        element = findChildNode(exchangedDocument, EInvoiceNS.RAM, "TypeCode");
        if (element != null) {
            setInvoiceTypeCode(element.getTextContent());
        }

        // read Date time
        element = findChildNode(exchangedDocument, EInvoiceNS.RAM, "IssueDateTime");
        if (element != null) {
            Element dateTimeElement = findChildNode(element, EInvoiceNS.UDT, "DateTimeString");
            if (dateTimeElement != null) {
                String dateStr = dateTimeElement.getTextContent();
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                    setIssueDateTime(LocalDate.parse(dateStr, formatter));
                } catch (DateTimeParseException e) {
                    // not parsable
                }
            }
        }

        // read Total amount

        Element child = findChildNode(specifiedTradeSettlementHeaderMonetarySummation, EInvoiceNS.RAM,
                "GrandTotalAmount");
        if (child != null) {
            setGrandTotalAmount(new BigDecimal(child.getTextContent()));
        }
        child = findChildNode(specifiedTradeSettlementHeaderMonetarySummation, EInvoiceNS.RAM, "TaxTotalAmount");
        if (child != null) {
            setTaxTotalAmount(new BigDecimal(child.getTextContent()));
        }
        setNetTotalAmount(getGrandTotalAmount().subtract(getTaxTotalAmount().setScale(2, RoundingMode.HALF_UP)));

        // due date
        Element specifiedTradePaymentTermsElement = findChildNode(element, EInvoiceNS.RAM,
                "SpecifiedTradePaymentTerms");
        if (specifiedTradePaymentTermsElement != null) {
            Element dateTimeElement = findChildNode(specifiedTradePaymentTermsElement, EInvoiceNS.RAM,
                    "DueDateDateTime");
            if (dateTimeElement != null) {
                Element dateTimeElementString = findChildNode(dateTimeElement, EInvoiceNS.UDT,
                        "DateTimeString");
                if (dateTimeElementString != null) {
                    String dateStr = dateTimeElementString.getTextContent();
                    try {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
                        setDueDateTime(LocalDate.parse(dateStr, formatter));
                    } catch (DateTimeParseException e) {
                        // not parsable
                    }
                }
            }
        }

        // read ApplicableHeaderTradeAgreement - buyerReference
        Element buyerReferenceElement = findChildNode(applicableHeaderTradeAgreement, EInvoiceNS.RAM,
                "BuyerReference");
        if (buyerReferenceElement != null) {
            setBuyerReference(buyerReferenceElement.getTextContent());
        }
        // read oder reference id
        Element buyerOrderReferenceElement = findChildNode(applicableHeaderTradeAgreement, EInvoiceNS.RAM,
                "BuyerOrderReferencedDocument");
        if (buyerOrderReferenceElement != null) {
            Element issuerAssignedID = findChildNode(buyerOrderReferenceElement, EInvoiceNS.RAM,
                    "IssuerAssignedID");
            if (issuerAssignedID != null) {
                setOrderReferenceId(issuerAssignedID.getTextContent());
            }
        }

        Element tradePartyElement = findChildNode(applicableHeaderTradeAgreement, EInvoiceNS.RAM,
                "SellerTradeParty");
        if (tradePartyElement != null) {
            getTradeParties().add(parseTradeParty(tradePartyElement, "seller"));
        }
        tradePartyElement = findChildNode(applicableHeaderTradeAgreement, EInvoiceNS.RAM,
                "BuyerTradeParty");
        if (tradePartyElement != null) {
            getTradeParties().add(parseTradeParty(tradePartyElement, "buyer"));
        }

        // read ShipToTradeParty from ApplicableHeaderTradeDelivery
        tradePartyElement = findChildNode(applicableHeaderTradeDelivery, EInvoiceNS.RAM, "ShipToTradeParty");
        if (tradePartyElement != null) {
            getTradeParties().add(parseTradeParty(tradePartyElement, "ship_to"));
        }

        // read line items...
        Set<TradeLineItem> lineItems = parseTradeLineItems();
        this.setTradeLineItems(lineItems);

    }

    public TradeParty parseTradeParty(Element tradePartyElement, String type) {
        TradeParty tradeParty = new TradeParty(type);
        Element element = null;

        // Parse name
        element = findChildNode(tradePartyElement, EInvoiceNS.RAM,
                "Name");
        if (element != null) {
            tradeParty.setName(element.getTextContent());
        }

        Element postalAddress = findChildNode(tradePartyElement, EInvoiceNS.RAM,
                "PostalTradeAddress");
        if (postalAddress != null) {
            element = findChildNode(postalAddress, EInvoiceNS.RAM,
                    "PostcodeCode");
            if (element != null) {
                tradeParty.setPostcodeCode(element.getTextContent());
            }
            element = findChildNode(postalAddress, EInvoiceNS.RAM,
                    "CityName");
            if (element != null) {
                tradeParty.setCityName(element.getTextContent());
            }
            element = findChildNode(postalAddress, EInvoiceNS.RAM,
                    "CountryID");
            if (element != null) {
                tradeParty.setCountryId(element.getTextContent());
            }
            element = findChildNode(postalAddress, EInvoiceNS.RAM,
                    "LineOne");
            if (element != null) {
                tradeParty.setStreetAddress(element.getTextContent());
            }
        }

        Element specifiedTaxRegistration = findChildNode(tradePartyElement, EInvoiceNS.RAM,
                "SpecifiedTaxRegistration");
        if (specifiedTaxRegistration != null) {
            element = findChildNode(specifiedTaxRegistration, EInvoiceNS.RAM,
                    "ID");
            if (element != null) {
                tradeParty.setVatNumber(element.getTextContent());
            }
        }
        return tradeParty;
    }

    /**
     * Parse the trade line items and return a list of items collected
     * 
     * @return
     */
    public Set<TradeLineItem> parseTradeLineItems() {
        Set<TradeLineItem> items = new LinkedHashSet<>();

        Set<Element> lineItems = findChildNodesByName(supplyChainTradeTransaction, EInvoiceNS.RAM,
                "IncludedSupplyChainTradeLineItem");

        for (Element lineItem : lineItems) {
            // Get Line ID
            Element docLine = findChildNode(lineItem, EInvoiceNS.RAM, "AssociatedDocumentLineDocument");
            if (docLine == null)
                continue;

            Element idElement = findChildNode(docLine, EInvoiceNS.RAM, "LineID");
            if (idElement == null)
                continue;

            TradeLineItem item = new TradeLineItem(idElement.getTextContent());

            // Product details
            Element product = findChildNode(lineItem, EInvoiceNS.RAM, "SpecifiedTradeProduct");
            if (product != null) {
                Element nameElement = findChildNode(product, EInvoiceNS.RAM, "Name");
                if (nameElement != null) {
                    item.setName(nameElement.getTextContent());
                }
                Element descElement = findChildNode(product, EInvoiceNS.RAM, "Description");
                if (descElement != null) {
                    item.setDescription(descElement.getTextContent());
                }
            }

            // Price info
            Element agreement = findChildNode(lineItem, EInvoiceNS.RAM, "SpecifiedLineTradeAgreement");
            if (agreement != null) {
                Element grossPrice = findChildNode(agreement, EInvoiceNS.RAM, "GrossPriceProductTradePrice");
                if (grossPrice != null) {
                    Element amount = findChildNode(grossPrice, EInvoiceNS.RAM, "ChargeAmount");
                    if (amount != null) {
                        item.setGrossPrice(Double.parseDouble(amount.getTextContent()));
                    }
                }

                Element netPrice = findChildNode(agreement, EInvoiceNS.RAM, "NetPriceProductTradePrice");
                if (netPrice != null) {
                    Element amount = findChildNode(netPrice, EInvoiceNS.RAM, "ChargeAmount");
                    if (amount != null) {
                        item.setNetPrice(Double.parseDouble(amount.getTextContent()));
                    }
                }

                // order ref id -
                // ram:SpecifiedLineTradeAgreement/ram:BuyerOrderReferencedDocument/ram:LineID
                Element buyerOrderReferencedDocument = findChildNode(agreement, EInvoiceNS.RAM,
                        "BuyerOrderReferencedDocument");
                if (buyerOrderReferencedDocument != null) {
                    Element lineIDElement = findChildNode(buyerOrderReferencedDocument, EInvoiceNS.RAM, "LineID");
                    if (lineIDElement != null) {
                        item.setOrderReferenceId(lineIDElement.getTextContent());
                    }

                }

            }

            // Quantity
            Element delivery = findChildNode(lineItem, EInvoiceNS.RAM, "SpecifiedLineTradeDelivery");
            if (delivery != null) {
                Element quantity = findChildNode(delivery, EInvoiceNS.RAM, "BilledQuantity");
                if (quantity != null) {
                    item.setQuantity(Double.parseDouble(quantity.getTextContent()));
                }
            }

            // VAT and total
            Element settlement = findChildNode(lineItem, EInvoiceNS.RAM, "SpecifiedLineTradeSettlement");
            if (settlement != null) {

                Element tax = findChildNode(settlement, EInvoiceNS.RAM, "ApplicableTradeTax");
                if (tax != null) {
                    Element rate = findChildNode(tax, EInvoiceNS.RAM, "RateApplicablePercent");
                    if (rate != null) {
                        item.setTaxRate(Double.parseDouble(rate.getTextContent()));
                    }
                }

                Element summation = findChildNode(settlement, EInvoiceNS.RAM,
                        "SpecifiedTradeSettlementLineMonetarySummation");
                if (summation != null) {
                    Element total = findChildNode(summation, EInvoiceNS.RAM, "LineTotalAmount");
                    if (total != null) {
                        item.setTotal(Double.parseDouble(total.getTextContent()));
                    }
                }
            }
            items.add(item);
        }

        return items;

    }

    /**
     * Update Invoice Number
     */
    @Override
    public void setId(String value) {
        super.setId(value);
        Element element = findOrCreateChildNode(exchangedDocument, EInvoiceNS.RAM, "ID");
        element.setTextContent(value);
    }

    /**
     * Update TypeCode
     * 
     * 380 - Commercial Invoice (normale Handelsrechnung).
     * 381 - (Gutschrift / Credit Note),
     * 384 - (korrigierte Rechnung)
     * 389 - (Selbstfakturierte Rechnung). -->
     */
    @Override
    public void setInvoiceTypeCode(String value) {
        super.setInvoiceTypeCode(value);
        Element element = findOrCreateChildNode(exchangedDocument, EInvoiceNS.RAM, "TypeCode");
        element.setTextContent(value);
    }

    /**
     * Update Invoice date
     */
    @Override
    public void setIssueDateTime(LocalDate value) {
        super.setIssueDateTime(value);
        Element element = findOrCreateChildNode(exchangedDocument, EInvoiceNS.RAM,
                "IssueDateTime");
        Element dateTimeElement = findOrCreateChildNode(element, EInvoiceNS.UDT,
                "DateTimeString");
        dateTimeElement.setAttribute("format", "102");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        dateTimeElement.setTextContent(formatter.format(value));
    }

    @Override
    public void setOrderReferenceId(String value) {
        super.setOrderReferenceId(value);
        Element buyerOrderReferenceElement = findOrCreateChildNode(applicableHeaderTradeAgreement, EInvoiceNS.RAM,
                "BuyerOrderReferencedDocument");
        Element issuerAssignedID = findOrCreateChildNode(buyerOrderReferenceElement, EInvoiceNS.RAM,
                "IssuerAssignedID");
        issuerAssignedID.setTextContent(value);
    }

    /**
     * Update Buyer Reference (BT-10)
     * 
     * <ram:BuyerReference>n/a</ram:BuyerReference>
     */
    @Override
    public void setBuyerReference(String value) {
        super.setBuyerReference(value);
        updateElementValue(applicableHeaderTradeAgreement, EInvoiceNS.RAM, "BuyerReference", value);
    }

    /**
     * Updates the payment terms description (BT-20).
     * 
     * <ram:SpecifiedTradePaymentTerms>
     * <ram:Description>Payment due by 10.03.2025</ram:Description>
     * </ram:SpecifiedTradePaymentTerms>
     */
    public void setPaymentTermsDescription(String value) {
        Element paymentTerms = findOrCreateChildNode(applicableHeaderTradeSettlement, EInvoiceNS.RAM,
                "SpecifiedTradePaymentTerms");
        updateElementValue(paymentTerms, EInvoiceNS.RAM, "Description", value);
    }

    /**
     * Update the invoice currency code
     * 
     * <ram:InvoiceCurrencyCode>EUR</ram:InvoiceCurrencyCode>
     */
    @Override
    public void setCurrency(String value) {
        super.setCurrency(value);
        updateElementValue(applicableHeaderTradeSettlement, EInvoiceNS.RAM, "InvoiceCurrencyCode", value);
    }

    /**
     * Update Duedate
     */
    @Override
    public void setDueDateTime(LocalDate value) {
        super.setDueDateTime(value);
        Element specifiedTradePaymentTermsElement = findOrCreateChildNode(applicableHeaderTradeSettlement,
                EInvoiceNS.RAM,
                "SpecifiedTradePaymentTerms");
        if (specifiedTradePaymentTermsElement != null) {
            Element dueDateTimeElement = findOrCreateChildNode(specifiedTradePaymentTermsElement,
                    EInvoiceNS.RAM,
                    "DueDateDateTime");
            Element dateTimeElement = findOrCreateChildNode(dueDateTimeElement, EInvoiceNS.UDT,
                    "DateTimeString");
            dateTimeElement.setAttribute("format", "102");
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            dateTimeElement.setTextContent(formatter.format(value));
        }

    }

    /**
     * Sets the actual delivery date (BT-72).
     * <p>
     * <ram:ActualDeliverySupplyChainEvent>
     * <ram:OccurrenceDateTime>
     * <udt:DateTimeString format="102">20261008</udt:DateTimeString>
     * </ram:OccurrenceDateTime>
     * </ram:ActualDeliverySupplyChainEvent>
     * 
     * @param value the delivery date
     */
    public void setDeliveryDate(LocalDate value) {
        if (value == null) {
            return;
        }
        Element event = findOrCreateChildNode(applicableHeaderTradeDelivery, EInvoiceNS.RAM,
                "ActualDeliverySupplyChainEvent");
        Element occurrence = findOrCreateChildNode(event, EInvoiceNS.RAM, "OccurrenceDateTime");
        Element dateTimeElement = findOrCreateChildNode(occurrence, EInvoiceNS.UDT, "DateTimeString");
        dateTimeElement.setAttribute("format", "102");
        dateTimeElement.setTextContent(DateTimeFormatter.ofPattern("yyyyMMdd").format(value));
    }

    /**
     * Updates or creates a trade party in the model and XML structure
     * 
     * @param newParty the trade party to be set
     */
    /**
     * Updates or creates a trade party in the model and XML structure
     * 
     * @param newParty the trade party to be set
     */
    @Override
    public void setTradeParty(TradeParty newParty) {
        if (newParty == null) {
            return;
        }

        // First update the model
        super.setTradeParty(newParty);

        Element parentElement;
        String elementName;

        // Determine parent element and party element name based on type
        if ("ship_to".equals(newParty.getType())) {
            parentElement = applicableHeaderTradeDelivery;
            elementName = "ShipToTradeParty";
        } else {
            parentElement = applicableHeaderTradeAgreement;
            elementName = newParty.getType().equals("seller") ? "SellerTradeParty" : "BuyerTradeParty";
        }

        if (parentElement != null) {
            Element tradePartyElement = findChildNode(parentElement, EInvoiceNS.RAM, elementName);

            // Create element if it doesn't exist
            if (tradePartyElement == null) {
                tradePartyElement = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + elementName);
                parentElement.appendChild(tradePartyElement);
            }

            // Update Name
            updateElementValue(tradePartyElement, EInvoiceNS.RAM, "Name", newParty.getName());

            // Update PostalTradeAddress
            Element postalAddress = findChildNode(tradePartyElement, EInvoiceNS.RAM, "PostalTradeAddress");
            if (postalAddress == null) {
                postalAddress = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "PostalTradeAddress");
                tradePartyElement.appendChild(postalAddress);
            }

            // Update address details

            // <ram:PostalTradeAddress>
            // <ram:PostcodeCode>12345</ram:PostcodeCode>
            // <ram:LineOne>Musterstraße 123</ram:LineOne>
            // <ram:CityName>Muster</ram:CityName>
            // <ram:CountryID>DE</ram:CountryID>
            // </ram:PostalTradeAddress>
            updateElementValue(postalAddress, EInvoiceNS.RAM, "PostcodeCode", newParty.getPostcodeCode());
            updateElementValue(postalAddress, EInvoiceNS.RAM, "LineOne", newParty.getStreetAddress());
            updateElementValue(postalAddress, EInvoiceNS.RAM, "CityName", newParty.getCityName());
            updateElementValue(postalAddress, EInvoiceNS.RAM, "CountryID", newParty.getCountryId());

            // Update VAT registration if available
            if (newParty.getVatNumber() != null && !newParty.getVatNumber().isEmpty()) {
                Element taxRegistration = findChildNode(tradePartyElement, EInvoiceNS.RAM,
                        "SpecifiedTaxRegistration");
                if (taxRegistration == null) {
                    taxRegistration = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "SpecifiedTaxRegistration");
                    tradePartyElement.appendChild(taxRegistration);
                }
                Element idElement = updateElementValue(taxRegistration, EInvoiceNS.RAM, "ID", newParty.getVatNumber());
                if (idElement != null) {
                    idElement.setAttribute("schemeID", "VA");
                }
            }
        }
    }

    /**
     * Adds a new TradeLineItem into the XML tree.
     * 
     * @param item
     */
    @Override
    public void addTradeLineItem(TradeLineItem item) {
        if (item == null) {
            return;
        }

        super.addTradeLineItem(item);

        // If an XML element for this LineID already exists (e.g. because
        // addTradeLineItem was called twice for the same position), remove
        // it first so we don't end up with duplicate line items in the XML.
        removeExistingLineItemElement(item.getId());

        // create main tags...
        // Insert before ApplicableHeaderTradeAgreement !!
        Element lineItem = createChildNode(supplyChainTradeTransaction, EInvoiceNS.RAM,
                "IncludedSupplyChainTradeLineItem", applicableHeaderTradeAgreement);
        Element associatedDocumentLineDocument = createChildNode(lineItem, EInvoiceNS.RAM,
                "AssociatedDocumentLineDocument");
        Element product = createChildNode(lineItem, EInvoiceNS.RAM, "SpecifiedTradeProduct");
        Element agreement = createChildNode(lineItem, EInvoiceNS.RAM, "SpecifiedLineTradeAgreement");
        Element delivery = createChildNode(lineItem, EInvoiceNS.RAM, "SpecifiedLineTradeDelivery");
        Element settlement = createChildNode(lineItem, EInvoiceNS.RAM,
                "SpecifiedLineTradeSettlement");

        // Document Line with ID
        updateElementValue(associatedDocumentLineDocument, EInvoiceNS.RAM, "LineID", item.getId());

        // Product details
        if (item.getName() != null) {
            updateElementValue(product, EInvoiceNS.RAM, "Name", item.getName());
        }
        if (item.getDescription() != null) {
            updateElementValue(product, EInvoiceNS.RAM, "Description", item.getDescription());
        }

        // order ref id -
        // /rsm:CrossIndustryInvoice/rsm:SupplyChainTradeTransaction/ram:IncludedSupplyChainTradeLineItem/ram:SpecifiedLineTradeAgreement/ram:BuyerOrderReferencedDocument/ram:LineID
        if (item.getOrderReferenceId() != null && !item.getOrderReferenceId().isEmpty()) {
            // ram:SpecifiedLineTradeAgreement/ram:BuyerOrderReferencedDocument/ram:LineID
            Element buyerOrderReferencedDocument = getDoc()
                    .createElement(getPrefix(EInvoiceNS.RAM) + "BuyerOrderReferencedDocument");
            Element lineIDElement = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "LineID");
            lineIDElement.setTextContent(item.getOrderReferenceId());
            buyerOrderReferencedDocument.appendChild(lineIDElement);
            agreement.appendChild(buyerOrderReferencedDocument);
        }

        // Trade Agreement (Prices)
        Element grossPrice = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "GrossPriceProductTradePrice");
        Element grossAmount = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "ChargeAmount");
        grossAmount.setTextContent(String.valueOf(item.getGrossPrice()));
        grossPrice.appendChild(grossAmount);
        agreement.appendChild(grossPrice);
        Element netPrice = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "NetPriceProductTradePrice");
        Element netAmount = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "ChargeAmount");
        netAmount.setTextContent(String.valueOf(item.getNetPrice()));
        netPrice.appendChild(netAmount);
        agreement.appendChild(netPrice);

        // Trade Delivery (Quantity)
        Element quantity = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "BilledQuantity");
        quantity.setAttribute("unitCode", "C62"); // Standard unit code
        quantity.setTextContent(String.valueOf(item.getQuantity()));
        delivery.appendChild(quantity);

        // Trade Settlement (VAT and Total)
        Element tax = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "ApplicableTradeTax");
        Element typeCode = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "TypeCode");
        typeCode.setTextContent("VAT");
        Element categoryCode = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "CategoryCode");
        if (item.getTaxRate() > 0) {
            categoryCode.setTextContent("S");
        } else {
            categoryCode.setTextContent("Z");
        }
        Element rate = getDoc().createElement(getPrefix(EInvoiceNS.RAM) + "RateApplicablePercent");
        rate.setTextContent(String.valueOf(item.getTaxRate()));
        tax.appendChild(typeCode);
        tax.appendChild(categoryCode);
        tax.appendChild(rate);
        settlement.appendChild(tax);

        // Update summary
        Element monetarySummation = createChildNode(settlement, EInvoiceNS.RAM,
                "SpecifiedTradeSettlementLineMonetarySummation");
        Element totalAmount = createChildNode(monetarySummation, EInvoiceNS.RAM,
                "LineTotalAmount");
        totalAmount.setTextContent(String.valueOf(item.getTotal()));

    }

    /**
     * Clears all trade line items from the model AND removes all
     * {@code IncludedSupplyChainTradeLineItem} XML elements from the DOM,
     * so model and XML stay in sync when an invoice is rebuilt from
     * scratch (e.g. re-processing the same template a second time).
     */
    @Override
    public void resetTradeLineItems() {
        super.resetTradeLineItems();
        Set<Element> existingLineItems = findChildNodesByName(supplyChainTradeTransaction,
                EInvoiceNS.RAM, "IncludedSupplyChainTradeLineItem");
        for (Element existing : existingLineItems) {
            supplyChainTradeTransaction.removeChild(existing);
        }
    }

    /**
     * Removes an existing {@code IncludedSupplyChainTradeLineItem} XML
     * element that has the given LineID, if present.
     * <p>
     * Used by {@link #addTradeLineItem(TradeLineItem)} to keep the XML in
     * sync with the model when a line item with the same ID is added
     * more than once.
     */
    private void removeExistingLineItemElement(String lineId) {
        if (lineId == null || lineId.isEmpty()) {
            return;
        }
        Set<Element> existingLineItems = findChildNodesByName(supplyChainTradeTransaction,
                EInvoiceNS.RAM, "IncludedSupplyChainTradeLineItem");
        for (Element existing : existingLineItems) {
            Element docLine = findChildNode(existing, EInvoiceNS.RAM, "AssociatedDocumentLineDocument");
            if (docLine == null) {
                continue;
            }
            Element idElement = findChildNode(docLine, EInvoiceNS.RAM, "LineID");
            if (idElement != null && lineId.equals(idElement.getTextContent())) {
                supplyChainTradeTransaction.removeChild(existing);
                break; // LineID is unique, no need to keep searching
            }
        }
    }

    /**
     * Writes the recalculated totals and the per-rate VAT breakdown into the
     * XML structure.
     * <p>
     * Called automatically by {@link EInvoiceModel#recalculateTotals()}
     * whenever a line item is added. Removes all existing
     * {@code ApplicableTradeTax} blocks on header level and re-creates one
     * block per distinct tax rate (BG-23 VAT BREAKDOWN), so invoices with
     * mixed tax rates are represented correctly.
     */
    @Override
    protected void updateTradeTax() {

        // Remove all existing ApplicableTradeTax blocks on header level -
        // they will be fully re-created below from the current breakdown.
        Set<Element> existingTaxBlocks = findChildNodesByName(applicableHeaderTradeSettlement,
                EInvoiceNS.RAM, "ApplicableTradeTax");
        for (Element existing : existingTaxBlocks) {
            applicableHeaderTradeSettlement.removeChild(existing);
        }
        // Determine the insertion point once, before the loop
        Element insertBeforeElement = findChildNode(applicableHeaderTradeSettlement, EInvoiceNS.RAM,
                "SpecifiedTradePaymentTerms");
        if (insertBeforeElement == null) {
            insertBeforeElement = findChildNode(applicableHeaderTradeSettlement, EInvoiceNS.RAM,
                    "SpecifiedTradeSettlementHeaderMonetarySummation");
        }
        // Re-create one ApplicableTradeTax block per distinct tax rate
        for (TaxBreakdown breakdown : getTaxBreakdown()) {
            Element applicableTradeTax = createChildNode(applicableHeaderTradeSettlement,
                    EInvoiceNS.RAM, "ApplicableTradeTax", insertBeforeElement);

            updateElementValue(applicableTradeTax, EInvoiceNS.RAM, "CalculatedAmount",
                    breakdown.getTaxAmount().toPlainString());
            updateElementValue(applicableTradeTax, EInvoiceNS.RAM, "TypeCode", "VAT");
            updateElementValue(applicableTradeTax, EInvoiceNS.RAM, "BasisAmount",
                    breakdown.getBasisAmount().toPlainString());
            updateElementValue(applicableTradeTax, EInvoiceNS.RAM, "CategoryCode",
                    breakdown.getRate().doubleValue() > 0 ? "S" : "Z");
            updateElementValue(applicableTradeTax, EInvoiceNS.RAM, "RateApplicablePercent",
                    breakdown.getRate().toPlainString());
        }

        // Update the header monetary summation from the recalculated totals
        BigDecimal net = getNetTotalAmount();
        BigDecimal tax = getTaxTotalAmount();
        BigDecimal grand = getGrandTotalAmount();

        updateElementValue(specifiedTradeSettlementHeaderMonetarySummation, EInvoiceNS.RAM,
                "LineTotalAmount", net.toPlainString());
        updateElementValue(specifiedTradeSettlementHeaderMonetarySummation, EInvoiceNS.RAM,
                "TaxBasisTotalAmount", net.toPlainString());
        Element taxTotalElement = updateElementValue(specifiedTradeSettlementHeaderMonetarySummation, EInvoiceNS.RAM,
                "TaxTotalAmount", tax.toPlainString());
        if (taxTotalElement != null) {
            taxTotalElement.setAttribute("currencyID", getCurrency());
        }
        updateElementValue(specifiedTradeSettlementHeaderMonetarySummation, EInvoiceNS.RAM,
                "GrandTotalAmount", grand.toPlainString());
        updateElementValue(specifiedTradeSettlementHeaderMonetarySummation, EInvoiceNS.RAM,
                "DuePayableAmount", grand.toPlainString());
    }

    /**
     * Sets the electronic address of the buyer (BT-49).
     * <p>
     * The element must be placed after PostalTradeAddress and before
     * SpecifiedTaxRegistration to comply with the CII schema sequence.
     * 
     * <ram:URIUniversalCommunication>
     * <ram:URIID schemeID="EM">invoice@example.com</ram:URIID>
     * </ram:URIUniversalCommunication>
     * 
     * @param schemeId the electronic address scheme (e.g. "EM" for e-mail)
     * @param address  the electronic address
     */
    public void setBuyerElectronicAddress(String schemeId, String address) {
        if (address == null || address.isEmpty()) {
            return;
        }
        Element buyer = findChildNode(applicableHeaderTradeAgreement, EInvoiceNS.RAM, "BuyerTradeParty");
        if (buyer == null) {
            return;
        }
        Element uriCommunication = findChildNode(buyer, EInvoiceNS.RAM, "URIUniversalCommunication");
        if (uriCommunication == null) {
            Element taxRegistration = findChildNode(buyer, EInvoiceNS.RAM, "SpecifiedTaxRegistration");
            if (taxRegistration != null) {
                uriCommunication = createChildNode(buyer, EInvoiceNS.RAM, "URIUniversalCommunication",
                        taxRegistration);
            } else {
                uriCommunication = createChildNode(buyer, EInvoiceNS.RAM, "URIUniversalCommunication");
            }
        }
        Element uriId = updateElementValue(uriCommunication, EInvoiceNS.RAM, "URIID", address);
        if (uriId != null) {
            uriId.setAttribute("schemeID", schemeId);
        }
    }

}
