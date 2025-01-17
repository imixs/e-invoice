package org.imixs.einvoice;

public class EInvoiceFormatException extends Exception {
    private static final long serialVersionUID = 1L;

    public static String INVALID_FORMAT_EXCEPTION = "E-INVOICE_INVALID_FORMAT_EXCEPTION";
    private String format;
    private String namespace;

    public EInvoiceFormatException(String format, String namespace) {
        super("Unsupported invoice format: " + format + " (Namespace: " + namespace + ")");
        this.format = format;
        this.namespace = namespace;
    }

    public String getFormat() {
        return format;
    }

    public String getNamespace() {
        return namespace;
    }
}