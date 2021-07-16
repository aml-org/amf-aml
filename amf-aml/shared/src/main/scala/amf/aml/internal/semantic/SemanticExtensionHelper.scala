package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect

object SemanticExtensionHelper {

  def byPropertyTerm(dialect: Dialect): SemanticExtensionFinder =
    SemanticExtensionFinder(dialect.extensions(), new PropertyTermFieldExtractor(dialect))

  def byTargetFinder(dialect: Dialect): SemanticExtensionFinder =
    SemanticExtensionFinder(dialect.extensions(), new TargetFieldExtractor(dialect))

  def byNameFinder(dialect: Dialect): SemanticExtensionFinder =
    SemanticExtensionFinder(dialect.extensions(), NameFieldExtractor)

}
