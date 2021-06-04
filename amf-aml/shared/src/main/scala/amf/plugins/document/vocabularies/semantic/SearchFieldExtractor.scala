package amf.plugins.document.vocabularies.semantic

import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.{AnnotationMapping, SemanticExtension}

trait SearchFieldExtractor {
  def extractSearchField(extension: SemanticExtension): Option[String]
}

object NameFieldExtractor extends SearchFieldExtractor {
  override def extractSearchField(extension: SemanticExtension): Option[String] = extension.extensionName().option()
}

case class TargetFieldExtractor(private val annotationMappings: Map[String, AnnotationMapping])
    extends SearchFieldExtractor {

  def this(dialect: Dialect) = this(dialect.annotationMappings().map(mapping => mapping.id -> mapping).toMap)

  override def extractSearchField(extension: SemanticExtension): Option[String] =
    extension
      .extensionMappingDefinition()
      .option()
      .flatMap(mappingId => annotationMappings.get(mappingId))
      .flatMap(mapping => mapping.domain().option())
}

case class PropertyTermFieldExtractor(private val annotationMappings: Map[String, AnnotationMapping])
    extends SearchFieldExtractor {

  def this(dialect: Dialect) = this(dialect.annotationMappings().map(mapping => mapping.id -> mapping).toMap)

  override def extractSearchField(extension: SemanticExtension): Option[String] =
    extension
      .extensionMappingDefinition()
      .option()
      .flatMap(mappingId => annotationMappings.get(mappingId))
      .flatMap(mapping => mapping.nodePropertyMapping().option())
}
