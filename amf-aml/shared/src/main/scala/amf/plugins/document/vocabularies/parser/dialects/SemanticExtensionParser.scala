package amf.plugins.document.vocabularies.parser.dialects

import amf.core.model.domain.AmfScalar
import amf.core.parser.{Annotations, SearchScope}
import amf.plugins.document.vocabularies.metamodel.domain.SemanticExtensionModel
import amf.plugins.document.vocabularies.model.domain.SemanticExtension
import amf.validation.DialectValidations.DialectError
import org.yaml.model.{YMapEntry, YScalar, YType}

case class SemanticExtensionParser(entry: YMapEntry, parent: String)(implicit val ctx: DialectContext) {
  def parse(): Option[SemanticExtension] = {
    val name = entry.key.as[YScalar].text
    entry.value.tagType match {
      case YType.Str =>
        val definitionRawRef = entry.value.as[YScalar].text

        val extensionMappingDefinition =
          ctx.declarations.findAnnotationMappingOrError(entry.value)(definitionRawRef, SearchScope.All)

        Some {
          SemanticExtension()
            .withId(s"$parent/$name")
            .withExtensionName(name)
            .set(SemanticExtensionModel.ExtensionMappingDefinition,
                 AmfScalar(extensionMappingDefinition.id, Annotations(entry.value)),
                 Annotations(entry))
        }
      case t =>
        ctx.eh.violation(DialectError,
                         parent,
                         s"Invalid type $t (expected ${YType.Str}) for semantic extension node $name",
                         entry.value)
        None
    }
  }
}
