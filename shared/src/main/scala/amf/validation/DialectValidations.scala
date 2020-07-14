package amf.validation

import amf.core.validation.SeverityLevels._
import amf.ProfileName
import amf.core.validation.core.ValidationSpecification
import amf.core.validation.core.ValidationSpecification.AML_VALIDATION
import amf.core.vocabulary.Namespace
import amf.core.vocabulary.Namespace.AmfAml
import amf.plugins.features.validation.Validations

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

  val MissingTermSpecification = validation(
    "missing-vocabulary-term",
    "Missing vocabulary term"
  )

  val MissingPropertyTermSpecification = validation(
    "missing-property-vocabulary-term",
    "Missing property vocabulary term"
  )

  val MissingFragmentSpecification = validation(
    "missing-dialect-fragment",
    "Missing dialect fragment"
  )

  val MissingPropertyRangeSpecification = validation(
    "missing-node-mapping-range-term",
    "Missing property range term"
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

  val DuplicateTerm = validation("duplicate-term", "Vocabulary defines duplicate terms")

  override val levels: Map[String, Map[ProfileName, String]] = Map(
    ClosedShapeSpecificationWarning.id -> all(WARNING)
  )

  override val validations: List[ValidationSpecification] = List(
    ClosedShapeSpecification,
    DialectAmbiguousRangeSpecification,
    InconsistentPropertyRangeValueSpecification,
    MissingPropertyRangeSpecification,
    MissingTermSpecification,
    DifferentTermsInMapKey,
    MissingFragmentSpecification,
    MissingPropertySpecification,
    InvalidModuleType,
    MissingVocabulary,
    InvalidUnionType,
    InvalidDialectPatch,
    DialectError,
    GuidRangeWithoutUnique,
    DuplicateTerm
  )
}
