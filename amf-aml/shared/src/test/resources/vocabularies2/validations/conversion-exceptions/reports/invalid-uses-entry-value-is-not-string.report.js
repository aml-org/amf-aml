ModelId: file://amf-aml/shared/src/test/resources/vocabularies2/validations/conversion-exceptions/invalid-uses-entry-value-is-not-string.yaml
Profile: 
Conforms: false
Number of results: 2

Level: Violation

- Constraint: http://a.ml/vocabularies/amf/core#syaml-error
  Message: Expecting !!str, !!seq provided
  Severity: Violation
  Target: 
  Property: 
  Range: [(5,14)-(5,27)]
  Location: file://amf-aml/shared/src/test/resources/vocabularies2/validations/conversion-exceptions/invalid-uses-entry-value-is-not-string.yaml

- Constraint: http://a.ml/vocabularies/amf/core#unresolved-reference
  Message: File Not Found: EISDIR: illegal operation on a directory, read
  Severity: Violation
  Target: 
  Property: 
  Range: [(5,14)-(5,27)]
  Location: file://amf-aml/shared/src/test/resources/vocabularies2/validations/conversion-exceptions/invalid-uses-entry-value-is-not-string.yaml
