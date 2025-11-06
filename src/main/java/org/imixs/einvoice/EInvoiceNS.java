package org.imixs.einvoice;

/**
 * Defines the valid eInvocing namespaces.
 * <p>
 * NOTE: The primary namespace are CBC/CAC and RAM/RMS
 * 
 * 
 * // xmlns:a='urn:un:unece:uncefact:data:standard:QualifiedDataType:100'
 * // xmlns:rsm='urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100'
 * // xmlns:qdt='urn:un:unece:uncefact:data:standard:QualifiedDataType:10'
 * //
 * xmlns:ram='urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100'
 * // xmlns:xs='http://www.w3.org/2001/XMLSchema'
 * // xmlns:udt='urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100'>
 * 
 * KSEF is a special format for the Polnisch KSeF format
 * 
 * @author rsoika
 */
public enum EInvoiceNS {
    CBC, //
    CAC, //
    RSM, //
    QDT, //
    RAM, //
    KSEF, // KSeF FA_VAT (Poland)
    UDT;
}
