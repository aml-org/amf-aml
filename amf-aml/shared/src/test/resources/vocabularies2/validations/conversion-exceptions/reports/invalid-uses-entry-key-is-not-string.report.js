ModelId: file://amf-aml/shared/src/test/resources/vocabularies2/validations/conversion-exceptions/invalid-uses-entry-key-is-not-string.yaml
Profile: 
Conforms: false
Number of results: 3

Level: Violation

- Constraint: http://a.ml/vocabularies/amf/core#syaml-error
  Message: Expected scalar but found: ["something"]
  Severity: Violation
  Target: 
  Property: 
  Range: [(5,2)-(5,15)]
  Location: file://amf-aml/shared/src/test/resources/vocabularies2/validations/conversion-exceptions/invalid-uses-entry-key-is-not-string.yaml

- Constraint: http://a.ml/vocabularies/amf/core#syaml-error
  Message: YAML scalar expected
  Severity: Violation
  Target: 
  Property: 
  Range: [(5,2)-(5,15)]
  Location: file://amf-aml/shared/src/test/resources/vocabularies2/validations/conversion-exceptions/invalid-uses-entry-key-is-not-string.yaml

- Constraint: http://a.ml/vocabularies/amf/core#unresolved-reference
  Message: File Not Found: ENOENT: no such file or directory, open 'amf-aml/shared/src/test/resources/vocabularies2/validations/conversion-exceptions/asdasd'
  Severity: Violation
  Target: asdasd
  Property: 
  Range: [(5,17)-(5,23)]
  Location: file://amf-aml/shared/src/test/resources/vocabularies2/validations/conversion-exceptions/invalid-uses-entry-key-is-not-string.yaml
