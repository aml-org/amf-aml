package amf.aml.internal.validate

import amf.aml.internal.utils.DialectRegister
import amf.core.client.common.validation.ProfileName
import amf.core.internal.validation.EffectiveValidations
import amf.core.internal.validation.core.ValidationProfile

trait SemanticExtensionConstraints {

  protected def withSemanticExtensionsConstraints(
      validations: EffectiveValidations,
      constraints: Map[ProfileName, ValidationProfile]
  ): EffectiveValidations = {
    constraints
      .get(DialectRegister.SEMANTIC_EXTENSIONS_PROFILE)
      .map(profile => validations.someEffective(profile))
      .getOrElse(validations)
  }
}
