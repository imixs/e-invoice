## E-Invoice - XRechnung

The following sections introduces the XRechnung standard which extends the EN16931.

- **EN 16931** is the European standard for electronic invoices. It defines a common semantic data model and two permitted syntaxes (UBL and UN/CEFACT CII). It exists so that invoices can be exchanged across EU member states in a uniform way (EU Directive 2014/55/EU).

- **XRechnung** is the German CIUS (Core Invoice Usage Specification) of EN 16931. A CIUS only _restricts_ the standard, it never extends it. XRechnung adds stricter rules, for example mandatory fields such as the buyer reference (BT-10), the buyer's electronic address (BT-49) and a seller contact.

In short:

- Every valid XRechnung is a valid EN 16931 invoice.
- Not every valid EN 16931 invoice is a valid XRechnung (e.g. a plain Factur-X/ZUGFeRD EN 16931 profile or an invoice from another EU country).

Choose the validation target accordingly: XRechnung is only required if the recipient demands it (e.g. German public authorities). For invoices from other EU countries, EN 16931 conformity is the relevant check.

## Validating an invoice with the KoSIT Validator

The [KoSIT Validator](https://github.com/itplr-kosit/validator) is the reference tool for checking invoices. It is only an engine; the actual rules come from a separate configuration.

### Setup

1. Download the latest release of the validator from
   https://github.com/itplr-kosit/validator/releases
   and use `validator-<version>-standalone.jar`.
2. Download the latest XRechnung configuration from
   https://github.com/itplr-kosit/validator-configuration-xrechnung/releases
   and unpack it.

### Run

```
java -jar validator-<version>-standalone.jar \
  -s scenarios.xml \
  -r ./<config-dir> \
  -o ./reports \
  factur-x.xml
```

- `-s` path to the scenario file of the configuration
- `-r` base directory of the configuration
- `-o` output directory for the generated reports

Run `java -jar validator-<version>-standalone.jar --help` for all options.

### Reading the result

The report lists one result per validation step:

| Step                 | Checks                                  |
| -------------------- | --------------------------------------- |
| XML Schema           | Syntax (UBL or CII)                     |
| Schematron EN 16931  | Conformity to the European standard     |
| Schematron XRechnung | Additional German CIUS rules (BR-DE-\*) |

If only the XRechnung step reports findings, the invoice is a valid EN 16931 invoice but not (yet) a valid XRechnung.
