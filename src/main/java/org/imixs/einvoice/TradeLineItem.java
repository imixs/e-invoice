package org.imixs.einvoice;

/**
 * A TradeParty is a container for a TradeParty element.
 * 
 * @author rsoika
 *
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

    public TradeLineItem(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getGrossPrice() {
        return grossPrice;
    }

    public void setGrossPrice(double grossPrice) {
        this.grossPrice = grossPrice;
    }

    public double getNetPrice() {
        return netPrice;
    }

    public void setNetPrice(double netPrice) {
        this.netPrice = netPrice;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getOrderReferenceId() {
        return orderReferenceId;
    }

    public void setOrderReferenceId(String orderReferenceId) {
        this.orderReferenceId = orderReferenceId;
    }

    // toString method for easy debugging
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
