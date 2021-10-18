package amf.aml.internal.semantic

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{AnnotationMapping, SemanticExtension}

object SemanticExtensionHelper {

  def getExtensionsRegistry(config: AMLConfiguration): Map[String, Dialect] = config.registry.getExtensionRegistry

  def getExtensions(config: AMLConfiguration): Seq[SemanticExtension] =
    getExtensionsRegistry(config).flatMap(e => e._2.extensions().find(ex => ex.extensionName().value() == e._1)).toSeq

  def findSemanticExtension(extensionName: String): Option[SemanticExtension] =
    dialect.extensions().find(e => e.extensionName().value() == extensionName)

  def findAnnotationMapping(dialect: Dialect, extension: SemanticExtension): AnnotationMapping =
    dialect.annotationMappings().filter(am => am.id == extension.extensionMappingDefinition().value()).head

  def byPropertyTerm(config: AMLConfiguration): Seq[SemanticExtensionFinder] =
    getExtensionsRegistry(config)
      .map(e => SemanticExtensionFinder(byNameFinder(config).find(e._1), new PropertyTermFieldExtractor(e._2)))
      .toSeq

  def byTargetFinder(config: AMLConfiguration): Seq[SemanticExtensionFinder] =
    getExtensionsRegistry(config)
      .map(e => SemanticExtensionFinder(byNameFinder(config).find(e._1), new TargetFieldExtractor(e._2)))
      .toSeq

  def byNameFinder(config: AMLConfiguration): SemanticExtensionFinder =
    SemanticExtensionFinder(getExtensions(config), NameFieldExtractor)

}
