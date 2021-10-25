package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{AnnotationMapping, SemanticExtension}

trait SearchFieldExtractor {
  def extractSearchField(extension: SemanticExtension): Option[String]
}

object NameFieldExtractor extends SearchFieldExtractor {
  override def extractSearchField(extension: SemanticExtension): Option[String] = extension.extensionName().option()
}

object TargetFieldExtractor {
  def apply(dialect: Dialect) = new TargetFieldExtractor(annotationMappingIndex(dialect))

  def apply(dialects: Seq[Dialect]): TargetFieldExtractor = {
    val mappingIndex = dialects.flatMap(_.annotationMappings()).map(x => x.id -> x).toMap
    new TargetFieldExtractor(mappingIndex)
  }

  private def annotationMappingIndex(dialect: Dialect): Map[String, AnnotationMapping] = {
    dialect.annotationMappings().map(mapping => mapping.id -> mapping).toMap
  }
}

case class TargetFieldExtractor(private val annotationMappings: Map[String, AnnotationMapping])
    extends SearchFieldExtractor {

  override def extractSearchField(extension: SemanticExtension): Option[String] =
    extension
      .extensionMappingDefinition()
      .option()
      .flatMap(mappingId => annotationMappings.get(mappingId))
      .flatMap(mapping => mapping.domain().option())
}

object PropertyTermFieldExtractor {
  def apply(dialect: Dialect) = new PropertyTermFieldExtractor(annotationMappingIndex(dialect))

  def apply(dialects: Seq[Dialect]) = {
    val mappingIndex = dialects.flatMap(_.annotationMappings()).map(x => x.id -> x).toMap
    new PropertyTermFieldExtractor(mappingIndex)
  }

  private def annotationMappingIndex(dialect: Dialect): Map[String, AnnotationMapping] = {
    dialect.annotationMappings().map(mapping => mapping.id -> mapping).toMap
  }
}

case class PropertyTermFieldExtractor(private val annotationMappings: Map[String, AnnotationMapping])
    extends SearchFieldExtractor {

  override def extractSearchField(extension: SemanticExtension): Option[String] =
    extension
      .extensionMappingDefinition()
      .option()
      .flatMap(mappingId => annotationMappings.get(mappingId))
      .flatMap(mapping => mapping.nodePropertyMapping().option())
}
