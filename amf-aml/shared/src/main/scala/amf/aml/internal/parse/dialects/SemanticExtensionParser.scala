package amf.aml.internal.parse.dialects

import amf.core.client.scala.model.domain.AmfScalar
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import amf.aml.internal.metamodel.domain.SemanticExtensionModel
import amf.aml.client.scala.model.domain.SemanticExtension
import amf.aml.internal.validate.DialectValidations.DialectError
import org.yaml.model.{YMapEntry, YScalar, YType}

case class SemanticExtensionParser(entry: YMapEntry, parent: String)(implicit val ctx: DialectContext) {
  def parse(): SemanticExtension = {
    val name = entry.key.as[YScalar].text
    val semantic = SemanticExtension(Annotations(entry))
      .withId(s"$parent/$name")
      .set(SemanticExtensionModel.ExtensionName, AmfScalar(name, Annotations(entry.key)), Annotations(entry.key))
    populateSemanticExtension(name, semantic)
    semantic
  }

  private def populateSemanticExtension(name: String, semantic: SemanticExtension) = {
    entry.value.tagType match {
      case YType.Str =>
        val definitionRawRef = entry.value.as[YScalar].text

        val extensionMappingDefinition =
          ctx.declarations.findAnnotationMappingOrError(entry.value)(definitionRawRef, SearchScope.All)
        semantic
          .set(SemanticExtensionModel.ExtensionMappingDefinition,
               AmfScalar(extensionMappingDefinition.id, Annotations(entry.value)),
               Annotations(entry))

      case t =>
        ctx.eh.violation(DialectError,
                         parent,
                         s"Invalid type $t (expected ${YType.Str}) for semantic extension node $name",
                         entry.value.location)
    }
  }
}
