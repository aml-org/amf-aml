package amf.aml.internal.parse.dialects

import amf.core.client.scala.model.domain.AmfScalar
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import amf.aml.internal.metamodel.domain.SemanticExtensionModel
import amf.aml.client.scala.model.domain.SemanticExtension
import amf.aml.internal.validate.DialectValidations.DialectError
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
