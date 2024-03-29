package amf.aml.internal.validate

import amf.core.client.common.validation.ProfileName
import amf.core.client.common.validation.SeverityLevels.WARNING
import amf.core.client.scala.vocabulary.Namespace
import amf.core.client.scala.vocabulary.Namespace.AmfAml
import amf.core.internal.validation.Validations
import amf.core.internal.validation.core.ValidationSpecification
import amf.core.internal.validation.core.ValidationSpecification.AML_VALIDATION

// noinspection TypeAnnotation
object DialectValidations extends Validations {
  override val specification: String = AML_VALIDATION
  override val namespace: Namespace  = AmfAml

  val DialectError = validation(
    "dialect-error",
    "Dialect error"
  )

  val MissingVocabulary = validation(
    "missing-vocabulary",
    "Missing vocabulary"
  )

  val MissingVocabularyTerm = validation(
    "missing-vocabulary-term",
    "Missing vocabulary term"
  )

  val MissingBaseTerm = validation(
    "missing-base-term",
    "Missing base term"
  )

  val MissingClassTermSpecification = validation(
    "missing-class-term",
    "Missing class term"
  )

  val MissingPropertyTermSpecification = validation(
    "missing-property-term",
    "Missing property term"
  )

  val MissingFragmentSpecification = validation(
    "missing-dialect-fragment",
    "Missing dialect fragment"
  )

  val MissingPropertyRangeSpecification = validation(
    "missing-node-mapping-range-term",
    "Missing property range term"
  )

  val UnavoidableAmbiguity = validation(
    "unavoidable-ambiguity",
    "Unavoidable ambiguity"
  )

  val EventualAmbiguity = validation(
    "eventual-ambiguity",
    "Eventual ambiguity"
  )

  val DifferentTermsInMapKey = validation(
    "different-terms-in-mapkey",
    "Different terms in map key"
  )

  val InconsistentPropertyRangeValueSpecification = validation(
    "inconsistent-property-range-value",
    "Range value does not match the expected type"
  )

  val ClosedShapeSpecification = validation(
    "closed-shape",
    "Invalid property for node"
  )

  val ClosedShapeSpecificationWarning = validation(
    "closed-shape-warning",
    "Invalid property for node"
  )

  val MissingPropertySpecification = validation(
    "mandatory-property-shape",
    "Missing mandatory property"
  )

  val InvalidModuleType = validation(
    "invalid-module-type",
    "Invalid module type"
  )

  val DialectAmbiguousRangeSpecification = validation(
    "dialect-ambiguous-range",
    "Ambiguous entity range"
  )

  val InvalidUnionType = validation(
    "invalid-union-type",
    "Union should be a sequence"
  )

  val ExpectedVocabularyModule = validation(
    "expected-vocabulary-module",
    "Expected vocabulary module"
  )

  val InvalidDialectPatch = validation(
    "invalid-dialect-patch",
    "Invalid dialect patch"
  )

  val GuidRangeWithoutUnique = validation(
    "guid-scalar-non-unique",
    "GUID scalar type declared without unique constraint"
  )

  val PropertyMappingMustBeAMap = validation("property-mapping-must-be-a-map", "Property mapping must be a map")

  val DuplicateTerm = validation("duplicate-term", "Vocabulary defines duplicate terms")

  val VariablesDefinedInBase =
    validation("variables-defined-in-base", "idTemplate variables are overridable by $base directive")

  // is seems that these severities are not taken into account for aml profile, must be registered in AMFDialectValidations
  override val levels: Map[String, Map[ProfileName, String]] = Map(
    ClosedShapeSpecificationWarning.id  -> all(WARNING),
    MissingClassTermSpecification.id    -> all(WARNING),
    MissingPropertyTermSpecification.id -> all(WARNING),
    MissingVocabularyTerm.id            -> all(WARNING)
  )

  override val validations: List[ValidationSpecification] = List(
    ClosedShapeSpecification,
    DialectAmbiguousRangeSpecification,
    InconsistentPropertyRangeValueSpecification,
    MissingPropertyRangeSpecification,
    MissingClassTermSpecification,
    MissingPropertyTermSpecification,
    DifferentTermsInMapKey,
    MissingFragmentSpecification,
    MissingPropertySpecification,
    InvalidModuleType,
    MissingVocabulary,
    MissingVocabularyTerm,
    MissingBaseTerm,
    InvalidUnionType,
    InvalidDialectPatch,
    DialectError,
    GuidRangeWithoutUnique,
    DuplicateTerm
  )
}
