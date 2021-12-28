package amf.aml.internal.render.emitters.instances

import amf.aml.client.scala.model.domain.DialectDomainElement
import amf.aml.internal.registries.AMLRegistry
import amf.aml.internal.semantic.SemanticExtensionsFacade
import amf.core.client.common.position.Position
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.client.scala.model.domain.{AmfArray, ScalarNode}
import amf.core.internal.metamodel.domain.DomainElementModel
import amf.core.internal.render.BaseEmitters.{MapEntryEmitter, pos}
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.{YDocument, YType}

object CustomDomainPropertiesEmitter {

  def apply(node: DialectDomainElement, registry: AMLRegistry, ordering: SpecOrdering, renderOptions: RenderOptions)(
      implicit nodeMappableFinder: NodeMappableFinder): Seq[EntryEmitter] = {
    node.fields.get(DomainElementModel.CustomDomainProperties) match {
      case AmfArray(customDomainProperties, _) =>
        customDomainProperties.flatMap {
          case extension: DomainExtension if Option(extension.extension).isEmpty =>
            val name = extensionSyntax(extension.name.value())
            SemanticExtensionsFacade(registry).render(name, extension, node.meta.typeIri, ordering, renderOptions)
          case domainExtension: DomainExtension => emitScalarExtension(domainExtension)
        }
      case _ => Nil
    }
  }

  private def extensionSyntax(name: String): String = s"($name)"

  private def emitScalarExtension(extension: DomainExtension): Seq[EntryEmitter] = {
    val extensionName = extension.name.value()
    extension.`extension` match {
      case s: ScalarNode =>
        val extensionValue = s.value.value()
        val tagType        = dataTypeToYamlType(s)
        val position       = pos(extension.annotations)
        Seq(MapEntryEmitter(extensionName, extensionValue, tag = tagType, position = position))
      case _ => Nil
    }
  }

  private def dataTypeToYamlType(s: ScalarNode) = {
    s.dataType.value() match {
      case DataType.Integer  => YType.Int
      case DataType.Float    => YType.Float
      case DataType.Boolean  => YType.Bool
      case DataType.Nil      => YType.Null
      case DataType.DateTime => YType.Timestamp
      case _                 => YType.Str
    }
  }
}
