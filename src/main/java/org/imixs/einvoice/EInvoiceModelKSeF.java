package org.imixs.einvoice;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * EInvoiceModel implementation for Polish KSeF FA(3) format.
 * <p>
 * This model supports the Polish National e-Invoice System structure FA(3)
 * which is mandatory from 1 February 2026.
 * 
 * @author imixs.com
 */
public class EInvoiceModelKSeF extends EInvoiceModel {

    // FA(3) namespace - NEW!
    private static final String KSEF_NS = "http://crd.gov.pl/wzor/2025/06/25/13775/";
    private static final String KSEF_PREFIX = ""; // Default namespace, no prefix

    /**
     * Set of EU country codes for VAT ID prefixes (excluding Poland).
     * Note: Greece uses "EL" (not "GR") for VAT IDs.
     * Note: UK is no longer in the EU since Brexit.
     */
    private static final Set<String> EU_COUNTRY_CODES = Set.of(
            "AT", // Austria
            "BE", // Belgium
            "BG", // Bulgaria
            "CY", // Cyprus
            "CZ", // Czech Republic
            "DE", // Germany
            "DK", // Denmark
            "EE", // Estonia
            "EL", // Greece (uses EL for VAT, not GR!)
            "ES", // Spain
            "FI", // Finland
            "FR", // France
            "HR", // Croatia
            "HU", // Hungary
            "IE", // Ireland
            "IT", // Italy
            "LT", // Lithuania
            "LU", // Luxembourg
            "LV", // Latvia
            "MT", // Malta
            "NL", // Netherlands
            "PT", // Portugal
            "RO", // Romania
            "SE", // Sweden
            "SI", // Slovenia
            "SK" // Slovakia
    );

    // Main structure elements
    protected Element naglowek;
    protected Element podmiot1;
    protected Element podmiot2;
    protected Element fa;

    protected String taxType = null;

    public EInvoiceModelKSeF(Document doc) {
        super(doc);
    }

    /**
     * Set KSeF namespaces
     */
    @Override
    public void setNameSpaces() {
        // Set KSeF namespace
        setUri(EInvoiceNS.KSEF, KSEF_NS);
        setPrefix(EInvoiceNS.KSEF, KSEF_PREFIX);

        // Parse namespaces from root element
        NamedNodeMap attributes = getRoot().getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Node node = attributes.item(i);
            String nodeName = node.getNodeName();
            String nodeValue = node.getNodeValue();

            String prefix = null;
            if (nodeName.startsWith("xmlns:")) {
                prefix = nodeName.substring(6);
            } else if ("xmlns".equals(nodeName)) {
                prefix = "";
            }

            if (prefix != null && nodeValue.equals(KSEF_NS)) {
                logger.fine("...set KSeF namespace prefix: " + prefix);
                setPrefix(EInvoiceNS.KSEF, prefix);
            }
        }

        if (getPrefix(EInvoiceNS.KSEF) == null) {
            logger.warning("Required KSeF namespace not found in document!");
        }
    }

    /**
     * Parse content and find/create main structure elements
     */
    @Override
    public void parseContent() {
        // Parse standard tags...
        naglowek = findOrCreateChildNode(getRoot(), EInvoiceNS.KSEF, "Naglowek");
        podmiot1 = findOrCreateChildNode(getRoot(), EInvoiceNS.KSEF, "Podmiot1");
        podmiot2 = findOrCreateChildNode(getRoot(), EInvoiceNS.KSEF, "Podmiot2");
        fa = findOrCreateChildNode(getRoot(), EInvoiceNS.KSEF, "Fa");

        // Load e-invoice standard data
        loadDocumentCoreData();
    }

    /**
     * Load core invoice data from KSeF XML
     */
    private void loadDocumentCoreData() {
        Element element = null;

        // Invoice number (P_2)
        element = findChildNode(fa, EInvoiceNS.KSEF, "P_2");
        if (element != null) {
            setId(element.getTextContent());
        }

        // Invoice date (P_1)
        element = findChildNode(fa, EInvoiceNS.KSEF, "P_1");
        if (element != null && !element.getTextContent().isEmpty()) {
            String dateStr = element.getTextContent();
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                setIssueDateTime(LocalDate.parse(dateStr, formatter));
            } catch (DateTimeParseException e) {
                logger.warning("Could not parse invoice date: " + dateStr);
            }
        }

        // Due date (P_6 in FA(3)!)
        element = findChildNode(fa, EInvoiceNS.KSEF, "P_6");
        if (element != null && !element.getTextContent().isEmpty()) {
            String dateStr = element.getTextContent();
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                setDueDateTime(LocalDate.parse(dateStr, formatter));
            } catch (DateTimeParseException e) {
                logger.warning("Could not parse due date: " + dateStr);
            }
        }

        // Parse seller (Podmiot1)
        if (podmiot1 != null) {
            getTradeParties().add(parseTradeParty(podmiot1, "seller"));
        }

        // Parse buyer (Podmiot2)
        if (podmiot2 != null) {
            getTradeParties().add(parseTradeParty(podmiot2, "buyer"));
        }

        // Parse totals
        parseTotal();

        // Parse line items
        Set<TradeLineItem> lineItems = parseTradeLineItems();
        this.setTradeLineItems(lineItems);
    }

    /**
     * This helper method allows to set the KSeF field 'RodzajFaktury'
     * 
     * Expected values are VAT, KOR, ZAL, ROZ, UPR, KOR_ZAL, KOR_ROZ
     * 
     * @param type
     */
    public void setRodzajFaktury(String type) {
        updateElementValue(fa, EInvoiceNS.KSEF, "RodzajFaktury", type);
    }

    /**
     * This helper method returns the KSeF field 'RodzajFaktury'
     * 
     * Expected values are VAT, KOR, ZAL, ROZ, UPR, KOR_ZAL, KOR_ROZ
     * 
     * Default is 'VAT'
     * 
     * @return
     */
    public String getRodzajFaktury() {
        String result = null;
        Element element = findChildNode(fa, EInvoiceNS.KSEF, "RodzajFaktury");
        if (element != null) {
            result = element.getTextContent();
        }

        // Set Default VAT
        if (result == null || result.isBlank()) {
            result = "VAT";
        }

        return result;
    }

    /**
     * Parse monetary totals from Fa element
     */
    public void parseTotal() {
        Element element = null;

        // Net total (P_13_1)
        element = findChildNode(fa, EInvoiceNS.KSEF, "P_13_1");
        if (element != null) {
            setNetTotalAmount(new BigDecimal(element.getTextContent()));
        }

        // VAT total (P_14_1)
        element = findChildNode(fa, EInvoiceNS.KSEF, "P_14_1");
        if (element != null) {
            setTaxTotalAmount(new BigDecimal(element.getTextContent()));
        }

        // Gross total (P_15)
        element = findChildNode(fa, EInvoiceNS.KSEF, "P_15");
        if (element != null) {
            setGrandTotalAmount(new BigDecimal(element.getTextContent()));
        }
    }

    /**
     * Parse a trade party (Podmiot1 or Podmiot2)
     * FA(3) uses simplified address structure: AdresL1, AdresL2
     * 
     * @param podmiotElement
     * @param type
     * @return
     */
    public TradeParty parseTradeParty(Element podmiotElement, String type) {
        TradeParty tradeParty = new TradeParty(type);
        Element element = null;

        // Parse identification data
        Element daneIdent = findChildNode(podmiotElement, EInvoiceNS.KSEF, "DaneIdentyfikacyjne");
        if (daneIdent != null) {
            // NIP
            element = findChildNode(daneIdent, EInvoiceNS.KSEF, "NIP");
            if (element != null) {
                tradeParty.setVatNumber(element.getTextContent());
            }

            // Name
            element = findChildNode(daneIdent, EInvoiceNS.KSEF, "Nazwa");
            if (element != null) {
                tradeParty.setName(element.getTextContent());
            }
        }

        // Parse address (FA(3) simplified structure!)
        Element adresElement = findChildNode(podmiotElement, EInvoiceNS.KSEF, "Adres");
        if (adresElement != null) {
            // Country
            element = findChildNode(adresElement, EInvoiceNS.KSEF, "KodKraju");
            if (element != null) {
                tradeParty.setCountryId(element.getTextContent());
            }

            // Address Line 1 (street + number)
            element = findChildNode(adresElement, EInvoiceNS.KSEF, "AdresL1");
            if (element != null) {
                tradeParty.setStreetAddress(element.getTextContent());
            }

            // Address Line 2 (postal code + city)
            element = findChildNode(adresElement, EInvoiceNS.KSEF, "AdresL2");
            if (element != null) {
                String adresL2 = element.getTextContent();
                // Try to parse "87-607 Środa Śląska" format
                if (adresL2.contains(" ")) {
                    String[] parts = adresL2.split(" ", 2);
                    tradeParty.setPostcodeCode(parts[0]);
                    if (parts.length > 1) {
                        tradeParty.setCityName(parts[1]);
                    }
                } else {
                    tradeParty.setCityName(adresL2);
                }
            }
        }

        return tradeParty;
    }

    /**
     * Parse trade line items (FaWiersz elements)
     * FA(3) has different field structure than FA(2)!
     * 
     * @return
     */
    public Set<TradeLineItem> parseTradeLineItems() {
        Set<TradeLineItem> items = new LinkedHashSet<>();

        Set<Element> lineItems = findChildNodesByName(fa, EInvoiceNS.KSEF, "FaWiersz");

        for (Element lineItem : lineItems) {
            // Line number (NrWierszaFa)
            Element nrElement = findChildNode(lineItem, EInvoiceNS.KSEF, "NrWierszaFa");
            if (nrElement == null)
                continue;

            TradeLineItem item = new TradeLineItem(nrElement.getTextContent());

            // UUID (new in FA(3))
            Element uuidElement = findChildNode(lineItem, EInvoiceNS.KSEF, "UU_ID");
            if (uuidElement != null) {
                item.setOrderReferenceId(uuidElement.getTextContent());
            }

            // Description (P_7)
            Element descElement = findChildNode(lineItem, EInvoiceNS.KSEF, "P_7");
            if (descElement != null) {
                item.setName(descElement.getTextContent());
            }

            // Unit (P_8A - was P_9A in FA(2)!)
            Element unitElement = findChildNode(lineItem, EInvoiceNS.KSEF, "P_8A");
            if (unitElement != null) {
                item.setDescription(unitElement.getTextContent());
            }

            // Quantity (P_8B)
            Element quantityElement = findChildNode(lineItem, EInvoiceNS.KSEF, "P_8B");
            if (quantityElement != null) {
                item.setQuantity(Double.parseDouble(quantityElement.getTextContent()));
            }

            // Unit price (P_9A - was P_9B in FA(2)!)
            Element priceElement = findChildNode(lineItem, EInvoiceNS.KSEF, "P_9A");
            if (priceElement != null) {
                item.setGrossPrice(Double.parseDouble(priceElement.getTextContent()));
                item.setNetPrice(Double.parseDouble(priceElement.getTextContent()));
            }

            // Net amount (P_11)
            Element netElement = findChildNode(lineItem, EInvoiceNS.KSEF, "P_11");
            if (netElement != null) {
                item.setTotal(Double.parseDouble(netElement.getTextContent()));
            }

            // VAT rate (P_12)
            Element vatElement = findChildNode(lineItem, EInvoiceNS.KSEF, "P_12");
            if (vatElement != null) {
                item.setTaxRate(Double.parseDouble(vatElement.getTextContent()));
            }

            items.add(item);
        }

        return items;
    }

    // ========== SETTER METHODS ==========

    @Override
    public void setId(String value) {
        super.setId(value);
        Element element = findOrCreateChildNode(fa, EInvoiceNS.KSEF, "P_2");
        element.setTextContent(value);
    }

    @Override
    public void setIssueDateTime(LocalDate value) {
        super.setIssueDateTime(value);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // Set in Naglowek (DataWytworzeniaFa) - NOW with timestamp
        Element dataWytwElement = findOrCreateChildNode(naglowek, EInvoiceNS.KSEF, "DataWytworzeniaFa");
        dataWytwElement.setTextContent(LocalDateTime.now().toString() + "Z");

        // Set in Fa (P_1) - date only
        Element p1Element = findOrCreateChildNode(fa, EInvoiceNS.KSEF, "P_1");
        p1Element.setTextContent(formatter.format(value));
    }

    @Override
    public void setDueDateTime(LocalDate value) {
        super.setDueDateTime(value);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        // P_6 is now the due date in FA(3)!
        Element element = findOrCreateChildNode(fa, EInvoiceNS.KSEF, "P_6");
        element.setTextContent(formatter.format(value));
    }

    /**
     * Sets the net total amount in the correct field based on taxType.
     * <p>
     * Field mapping:
     * <ul>
     * <li>taxType "1" (Poland): P_13_1 (domestic VAT)</li>
     * <li>taxType "2" (EU): P_13_6_2 (intra-community delivery, 0%)</li>
     * <li>taxType "3" (Non-EU): P_13_6_3 (export, 0%)</li>
     * </ul>
     * 
     * @param value the net total amount
     */
    @Override
    public void setNetTotalAmount(BigDecimal value) {
        super.setNetTotalAmount(value);

        Element element;
        logger.info("│   ├──  setNetTotal for tax type=" + taxType);
        if ("2".equals(taxType)) {
            // EU: P_13_6_2 comes after P_13_6_1 (or after P_6 if no P_13_x exists)
            element = findOrCreateChildNodeAfter(fa, EInvoiceNS.KSEF, "P_13_6_2", "P_6");
            logger.info("│   ├──  set P_13_6_2 = " + value);
        } else if ("3".equals(taxType)) {
            // Export: P_13_6_3 comes after P_13_6_2
            element = findOrCreateChildNodeAfter(fa, EInvoiceNS.KSEF, "P_13_6_3", "P_6");
            logger.info("│   ├──  set P_13_6_3 = " + value);
        } else {
            // Poland: P_13_1 comes after P_6
            element = findOrCreateChildNodeAfter(fa, EInvoiceNS.KSEF, "P_13_1", "P_6");
            logger.info("│   ├──  set P_13_1 = " + value);
        }

        element.setTextContent(value.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    /**
     * Sets the tax total amount (P_14_1).
     * <p>
     * This field is only valid for domestic Polish invoices (taxType "1").
     * For EU (taxType "2") and export (taxType "3") invoices, no tax field
     * should be set as these are 0% VAT transactions.
     * 
     * @param value the tax total amount
     */
    @Override
    public void setTaxTotalAmount(BigDecimal value) {
        // Only set tax for domestic Polish invoices
        if ("2".equals(taxType) || "3".equals(taxType)) {
            return;
        }
        logger.info("│   ├──  setTaxTotalAmount for tax type=" + taxType + " P_14_1=" + value);
        super.setTaxTotalAmount(value);
        // P_14_1 must come directly after P_13_1
        Element element = findOrCreateChildNodeAfter(fa, EInvoiceNS.KSEF, "P_14_1", "P_13_1");
        element.setTextContent(value.setScale(2, RoundingMode.HALF_UP).toPlainString());

    }

    @Override
    public void setGrandTotalAmount(BigDecimal value) {
        super.setGrandTotalAmount(value);
        logger.info("│   ├──  setGrandTotalAmount for tax type=" + taxType);
        // Determine the correct predecessor based on taxType
        String afterElement;
        if ("2".equals(taxType)) {
            afterElement = "P_13_6_2";
        } else if ("3".equals(taxType)) {
            afterElement = "P_13_6_3";
        } else {
            // Poland: P_15 comes after P_14_1 (or P_13_1 if no tax)
            afterElement = "P_14_1";
            if (findChildNode(fa, EInvoiceNS.KSEF, "P_14_1") == null) {
                afterElement = "P_13_1";
            }
        }
        logger.info("│   ├──  set P_15 = " + value);
        Element element = findOrCreateChildNodeAfter(fa, EInvoiceNS.KSEF, "P_15", afterElement);
        element.setTextContent(value.setScale(2, RoundingMode.HALF_UP).toPlainString());

    }

    /**
     * Updates or creates a trade party (buyer/seller) in the model and XML
     * structure
     * FA(3) uses simplified address: AdresL1 (street+number), AdresL2 (postal+city)
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

        Element podmiotElement = null;

        // Determine which Podmiot element to update
        if ("seller".equals(newParty.getType())) {
            podmiotElement = podmiot1;
        } else if ("buyer".equals(newParty.getType())) {
            podmiotElement = podmiot2;
        }

        if (podmiotElement != null) {
            // Update DaneIdentyfikacyjne
            Element daneIdent = findOrCreateChildNode(podmiotElement, EInvoiceNS.KSEF, "DaneIdentyfikacyjne");

            // Update NIP
            if (newParty.getVatNumber() != null && !newParty.getVatNumber().isEmpty()) {

                String vatID = newParty.getVatNumber().trim().toUpperCase();
                vatID = vatID.replace(" ", "");
                // for PL set NIP otherwise set NrID
                if (vatID.startsWith("PL")) {
                    updateElementValue(daneIdent, EInvoiceNS.KSEF, "NIP", vatID.substring(2));
                } else {
                    updateElementValue(daneIdent, EInvoiceNS.KSEF, "NrID", vatID);
                }

            }

            // Update Name
            if (newParty.getName() != null && !newParty.getName().isEmpty()) {
                updateElementValue(daneIdent, EInvoiceNS.KSEF, "Nazwa", newParty.getName());
            }

            // Update Adres (FA(3) simplified structure!)
            Element adres = findOrCreateChildNode(podmiotElement, EInvoiceNS.KSEF, "Adres");

            // Update Country
            if (newParty.getCountryId() != null && !newParty.getCountryId().isEmpty()) {
                updateElementValue(adres, EInvoiceNS.KSEF, "KodKraju", newParty.getCountryId());
            }

            // Update AdresL1 (street + house number)
            if (newParty.getStreetAddress() != null && !newParty.getStreetAddress().isEmpty()) {
                updateElementValue(adres, EInvoiceNS.KSEF, "AdresL1", newParty.getStreetAddress());
            }

            // Update AdresL2 (postal code + city)
            if (newParty.getPostcodeCode() != null && newParty.getCityName() != null) {
                String adresL2 = newParty.getPostcodeCode() + " " + newParty.getCityName();
                updateElementValue(adres, EInvoiceNS.KSEF, "AdresL2", adresL2);
            }
        }
    }

    /**
     * Adds a new TradeLineItem into the XML tree.
     * Line items are inserted before Adnotacje or P_13_1.
     * FA(3) has different field mappings than FA(2)!
     * 
     * @param item
     */
    @Override
    public void setTradeLineItem(TradeLineItem item) {
        if (item == null) {
            return;
        }

        super.setTradeLineItem(item);

        // Create FaWiersz element (append in FA)
        Element faWiersz = createChildNode(fa, EInvoiceNS.KSEF, "FaWiersz");

        // Line number (NrWierszaFa)
        updateElementValue(faWiersz, EInvoiceNS.KSEF, "NrWierszaFa", item.getId());

        // UUID (new in FA(3)) - generate if not present
        String uuid = item.getOrderReferenceId();
        if (uuid == null || uuid.isEmpty()) {
            uuid = UUID.randomUUID().toString();
        }
        updateElementValue(faWiersz, EInvoiceNS.KSEF, "UU_ID", uuid);

        // Description (P_7)
        if (item.getName() != null && !item.getName().isEmpty()) {
            updateElementValue(faWiersz, EInvoiceNS.KSEF, "P_7", item.getName());
        }

        // Unit (P_8A in FA(3) - was P_9A in FA(2)!)
        String unit = item.getDescription();
        if (unit == null || unit.isEmpty()) {
            unit = "szt.";
        }
        updateElementValue(faWiersz, EInvoiceNS.KSEF, "P_8A", unit);

        // Quantity (P_8B)
        updateElementValue(faWiersz, EInvoiceNS.KSEF, "P_8B", String.valueOf((int) item.getQuantity()));

        // Unit price (P_9A in FA(3) - was P_9B in FA(2)!)
        updateElementValue(faWiersz, EInvoiceNS.KSEF, "P_9A", String.format("%.2f", item.getNetPrice()));

        // Net amount (P_11)
        updateElementValue(faWiersz, EInvoiceNS.KSEF, "P_11", String.format("%.2f", item.getTotal()));

        // VAT rate (P_12) if >0
        if (item.getTaxRate() > 0) {
            updateElementValue(faWiersz, EInvoiceNS.KSEF, "P_12", String.valueOf((int) item.getTaxRate()));
        }
    }

    /**
     * This helper method computes the KSeF tax type based on the VAT ID.
     * The tax type is used to compute the fields P_13_6_1, P_13_6_2, P_13_6_3
     * <p>
     * Possible values:
     * <ul>
     * <li>"1" - Poland (domestic, 0% special cases)</li>
     * <li>"2" - EU country (intra-community delivery / WDT)</li>
     * <li>"3" - Non-EU country (export)</li>
     * </ul>
     * 
     * @param vatID the VAT identification number with country prefix
     */
    public void setTaxType(String vatID) {

        logger.info("├── set tax type for : " + vatID);

        if (vatID == null || vatID.isBlank()) {
            logger.info("├── undefined");
            return;
        }

        String id = vatID.trim().toUpperCase();

        // Poland - domestic
        if (id.startsWith("PL")) {
            logger.info("├── 1=PL");
            this.taxType = "1";
            return;
        }

        // Check for EU country prefix (first 2 characters)
        if (id.length() >= 2) {
            String countryCode = id.substring(0, 2);
            if (EU_COUNTRY_CODES.contains(countryCode)) {
                logger.info("├── 2=EU");
                this.taxType = "2";
                return;
            }
        }

        // Non-EU country (export)
        logger.info("├── 3 NON EU");
        this.taxType = "3";
    }

    /**
     * This helper method returns the KSeF tax type for the fields
     * P_13_6_1, P_13_6_2, P_13_6_3
     * <p>
     * Possible values:
     * <ul>
     * <li>"1" - Poland (P_13_6_1)</li>
     * <li>"2" - EU country (P_13_6_2)</li>
     * <li>"3" - Non-EU country (P_13_6_3)</li>
     * </ul>
     * 
     * @return the tax type
     */
    public String getTaxType() {
        return this.taxType;
    }
}