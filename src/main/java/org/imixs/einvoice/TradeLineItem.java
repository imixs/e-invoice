package org.imixs.einvoice;

/**
 * A {@code TradeLineItem} represents a single invoice line (a position) of an
 * e-invoice.
 * <p>
 * The class is a plain, format-agnostic data container. It carries the
 * business values of one invoice position and deliberately does <b>not</b>
 * contain any XML logic. Concrete model implementations - such as
 * {@link EInvoiceModelCII}, {@link EInvoiceModelUBL} or
 * {@link EInvoiceModelKSeF} - are responsible for mapping these fields onto
 * their own, format-specific XML structure.
 * <p>
 * Because of this separation, new e-invoice formats can be implemented on top
 * of the same model without touching this class.
 *
 * <h2>Field semantics</h2>
 * The field names follow the terminology used by EN16931. The most important
 * distinction is between the <b>unit prices</b> and the <b>line total</b>:
 *
 * <table border="1">
 * <caption>Mapping of fields to EN16931 business terms</caption>
 * <tr>
 * <th>Field</th>
 * <th>Meaning</th>
 * <th>EN16931</th>
 * </tr>
 * <tr>
 * <td>{@link #getId() id}</td>
 * <td>Line position / line number</td>
 * <td>BT-126</td>
 * </tr>
 * <tr>
 * <td>{@link #getName() name}</td>
 * <td>Item name</td>
 * <td>BT-153</td>
 * </tr>
 * <tr>
 * <td>{@link #getDescription() description}</td>
 * <td>Item description</td>
 * <td>BT-154</td>
 * </tr>
 * <tr>
 * <td>{@link #getQuantity() quantity}</td>
 * <td>Billed quantity</td>
 * <td>BT-129</td>
 * </tr>
 * <tr>
 * <td>{@link #getGrossPrice() grossPrice}</td>
 * <td>List price (before discounts)</td>
 * <td>BT-148</td>
 * </tr>
 * <tr>
 * <td>{@link #getNetPrice() netPrice}</td>
 * <td>Net unit price actually charged</td>
 * <td>BT-146</td>
 * </tr>
 * <tr>
 * <td>{@link #getTaxRate() taxRate}</td>
 * <td>VAT rate in percent (e.g. {@code 19.0})</td>
 * <td>BT-152</td>
 * </tr>
 * <tr>
 * <td>{@link #getTotal() total}</td>
 * <td><b>Net</b> total amount of this line</td>
 * <td>BT-131</td>
 * </tr>
 * <tr>
 * <td>{@link #getOrderReferenceId() orderReferenceId}</td>
 * <td>Optional order line reference</td>
 * <td>BT-132</td>
 * </tr>
 * </table>
 *
 * <h2>Important: totals are recalculated by the model</h2>
 * When a {@code TradeLineItem} is added to an {@link EInvoiceModel} via
 * {@link EInvoiceModel#addTradeLineItem(TradeLineItem)}, the header totals
 * (net, tax and grand total) as well as the per-rate VAT breakdown are
 * <b>recalculated from the line items</b>. Setting the header totals
 * manually on the model has no lasting effect once line items are added.
 *
 * @author rsoika
 * @see EInvoiceModel#addTradeLineItem(TradeLineItem)
 * @see EInvoiceModel#getTaxBreakdown()
 */
public class TradeLineItem {

    private String id;
    private String name;
    private String description;
    private double grossPrice;
    private double netPrice;
    private double quantity;
    private double taxRate;
    private double total;
    private String orderReferenceId; // Order-ID

    /**
     * Constructs a new line item with the given line id (BT-126).
     * <p>
     * If a {@code null} or blank id is passed here, the enclosing
     * {@link EInvoiceModel} will assign a fallback id based on the
     * current number of line items when the item is added via
     * {@link EInvoiceModel#addTradeLineItem(TradeLineItem)}.
     *
     * @param id the line id / position number; may be {@code null}
     */
    public TradeLineItem(String id) {
        this.id = id;
    }

    /**
     * Returns the line id / line number (BT-126).
     *
     * @return the line id, or {@code null} if not set
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the line id / line number (BT-126).
     *
     * @param id the line id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the item name (BT-153).
     *
     * @return the item name, or {@code null} if not set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the item name (BT-153).
     *
     * @param name the item name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the item description (BT-154).
     *
     * @return the item description, or {@code null} if not set
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the item description (BT-154).
     *
     * @param description the item description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the gross unit price / list price (BT-148).
     * <p>
     * This is the unit price <b>before</b> any discounts or surcharges and
     * <b>without</b> VAT. It is not the same as the "brutto" price in the
     * German colloquial sense - in EN16931 this is the item's list price.
     *
     * @return the gross (list) unit price
     */
    public double getGrossPrice() {
        return grossPrice;
    }

    /**
     * Sets the gross unit price / list price (BT-148).
     * <p>
     * This is the unit price <b>before</b> any discounts or surcharges.
     * Depending on the target format, this value may or may not be written
     * to the generated XML. For example:
     * <ul>
     * <li>CII writes it to {@code GrossPriceProductTradePrice/ChargeAmount}</li>
     * <li>KSeF FA(3) does <b>not</b> write this field at all</li>
     * </ul>
     * See the concrete model implementation for the exact mapping.
     *
     * @param grossPrice the gross (list) unit price
     */
    public void setGrossPrice(double grossPrice) {
        this.grossPrice = grossPrice;
    }

    /**
     * Returns the net unit price (BT-146).
     * <p>
     * This is the unit price actually charged, <b>excluding</b> VAT.
     *
     * @return the net unit price
     */
    public double getNetPrice() {
        return netPrice;
    }

    /**
     * Sets the net unit price (BT-146).
     * <p>
     * This value is <b>required</b> in most formats and must be set in
     * addition to {@link #setGrossPrice(double)}. If it is left at its
     * default ({@code 0.0}), the generated XML will contain a net unit
     * price of {@code 0.0}, which usually fails validation.
     * <p>
     * For example:
     * <ul>
     * <li>CII writes it to {@code NetPriceProductTradePrice/ChargeAmount}</li>
     * <li>UBL writes it to {@code cac:Price/cbc:PriceAmount}</li>
     * <li>KSeF FA(3) writes it to {@code P_9A}</li>
     * </ul>
     *
     * @param netPrice the net unit price
     */
    public void setNetPrice(double netPrice) {
        this.netPrice = netPrice;
    }

    /**
     * Returns the billed quantity (BT-129).
     *
     * @return the billed quantity
     */
    public double getQuantity() {
        return quantity;
    }

    /**
     * Sets the billed quantity (BT-129).
     *
     * @param quantity the billed quantity
     */
    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    /**
     * Returns the VAT rate in percent (BT-152), e.g. {@code 19.0}.
     * <p>
     * A value of {@code 0.0} indicates a zero-rated or exempt line.
     *
     * @return the VAT rate in percent
     */
    public double getTaxRate() {
        return taxRate;
    }

    /**
     * Sets the VAT rate in percent (BT-152).
     * <p>
     * Note: the method is called {@code setTaxRate} - <b>not</b>
     * {@code setVat}. Use e.g. {@code 19.0} for the standard German VAT
     * rate.
     *
     * @param taxRate the VAT rate in percent
     */
    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    /**
     * Returns the <b>net</b> total amount of this line (BT-131).
     * <p>
     * This is the line total <b>excluding</b> VAT, typically calculated as
     * {@code netPrice * quantity} (minus any line-level discounts).
     *
     * @return the net line total
     */
    public double getTotal() {
        return total;
    }

    /**
     * Sets the <b>net</b> total amount of this line (BT-131).
     * <p>
     * <b>Important:</b> despite the generic name, this is <b>not</b> the
     * gross amount. It is the net line total, excluding VAT. It maps to:
     * <ul>
     * <li>CII: {@code LineTotalAmount}</li>
     * <li>UBL: {@code cbc:LineExtensionAmount}</li>
     * <li>KSeF FA(3): {@code P_11}</li>
     * </ul>
     * The VAT for this line is added by the model when it recalculates the
     * header totals and builds the per-rate VAT breakdown.
     * <p>
     * If you want to express a gross line total, compute it yourself and
     * set the matching {@link #setTaxRate(double) tax rate} - the library
     * will add the tax on top when recalculating the header totals.
     *
     * @param total the net line total
     */
    public void setTotal(double total) {
        this.total = total;
    }

    /**
     * Returns the optional order line reference (BT-132).
     *
     * @return the order line reference, or {@code null} if not set
     */
    public String getOrderReferenceId() {
        return orderReferenceId;
    }

    /**
     * Sets the optional order line reference (BT-132).
     * <p>
     * Depending on the target format this value is written to different
     * elements, e.g. CII {@code BuyerOrderReferencedDocument/LineID} or
     * UBL {@code cac:OrderLineReference/cbc:LineID}.
     *
     * @param orderReferenceId the order line reference
     */
    public void setOrderReferenceId(String orderReferenceId) {
        this.orderReferenceId = orderReferenceId;
    }

    /**
     * Returns a debug representation of this line item.
     *
     * @return a string representation of all fields
     */
    @Override
    public String toString() {
        return "TradeLineItem{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", grossPrice='" + grossPrice + '\'' +
                ", netPrice='" + netPrice + '\'' +
                ", quantity='" + quantity + '\'' +
                ", taxRate='" + taxRate + '\'' +
                ", total='" + total + '\'' +
                '}';
    }
}