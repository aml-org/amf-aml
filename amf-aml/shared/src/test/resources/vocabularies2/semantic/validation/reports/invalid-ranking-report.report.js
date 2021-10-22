ModelId: file://amf-aml/shared/src/test/resources/vocabularies2/semantic/validation/instances/invalid-ranking-instance.yaml
Profile: Github Repository 1.0
Conforms: false
Number of results: 1

Level: Violation

- Constraint: http://a.ml/vocabularies/data#file://amf-aml/shared/src/test/resources/vocabularies2/semantic/validation/scalar-extensions.yaml#/declarations/RankingAnnotationMapping_ranking_maximum_validation
  Message: Property 'ranking' maximum inclusive value is 5
  Severity: Violation
  Target: file://amf-aml/shared/src/test/resources/vocabularies2/semantic/validation/instances/invalid-ranking-instance.yaml#/encodes
  Property: http://a.ml/vocab#ranking
  Range: [(3,11)-(3,12)]
  Location: file://amf-aml/shared/src/test/resources/vocabularies2/semantic/validation/instances/invalid-ranking-instance.yaml
