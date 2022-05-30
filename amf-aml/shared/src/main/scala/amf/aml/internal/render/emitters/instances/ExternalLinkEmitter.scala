package amf.aml.internal.render.emitters.instances

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping, PropertyLikeMapping, PropertyMapping}
import amf.aml.internal.annotations.{CustomBase, CustomId}
import amf.aml.internal.metamodel.domain.PropertyLikeMappingModel
import amf.aml.internal.registries.AMLRegistry
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfElement}
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import org.yaml.model.YDocument
import org.yaml.model.YDocument.PartBuilder

case class ExternalLinkEmitter[M <: PropertyLikeMappingModel](
    key: String,
    dialect: Dialect,
    target: AmfElement,
    propertyMapping: PropertyLikeMapping[M],
    annotations: Option[Annotations] = None,
    keyPropertyId: Option[String] = None,
    references: Seq[BaseUnit],
    ordering: SpecOrdering,
    renderOptions: RenderOptions,
    registry: AMLRegistry
)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AmlEmittersHelper {
  override def emit(b: YDocument.EntryBuilder): Unit = {
    b.entry(
        key,
        (e) => {
          target match {
            case array: AmfArray =>
              e.list(l => {
                array.values.asInstanceOf[Seq[DialectDomainElement]].foreach { elem =>
                  if (elem.fields.nonEmpty) { // map reference
                    nodeMappingForObjectProperty(propertyMapping, elem) match {
                      case Some(rangeMapping) =>
                        DialectNodeEmitter(
                            elem,
                            rangeMapping,
                            references,
                            dialect,
                            ordering,
                            discriminator = None,
                            keyPropertyId = keyPropertyId,
                            renderOptions = renderOptions,
                            registry = registry
                        ).emit(l)
                      case _ => // ignore, error
                    }
                  } else { // just link
                    emitCustomId(elem, l)
                    emitCustomBase(elem, l)
                  }
                }
              })
            case element: DialectDomainElement =>
              emitCustomId(element, e)
              emitCustomBase(element, e)
          }
        }
    )
  }

  protected def nodeMappingForObjectProperty(
      propertyMapping: PropertyLikeMapping[_],
      dialectDomainElement: DialectDomainElement
  ): Option[NodeMappable] = {
    // this can be multiple mappings if we have a union in the range or a range pointing to a union mapping
    val nodeMappings: Seq[NodeMapping] =
      propertyMapping.objectRange().flatMap { rangeNodeMapping =>
        findAllNodeMappings(rangeNodeMapping.value())
      }
    nodeMappings.find(nodeMapping =>
      dialectDomainElement.meta.`type`
        .map(_.iri())
        .exists(i => i == nodeMapping.nodetypeMapping.value() || i == nodeMapping.id)
    )
  }

  private def emitCustomId(elem: DialectDomainElement, b: PartBuilder): Unit = {
    elem.annotations.find(classOf[CustomId]) match {
      case Some(customId) if customId.value != "true" =>
        b.obj { m =>
          m.entry("$id", customId.value)
        }
      case Some(_) =>
        b.obj { m =>
          m.entry("$id", elem.id)
        }
      case _ => b += elem.id
    }
  }

  private def emitCustomBase(elem: DialectDomainElement, b: PartBuilder): Unit = {
    elem.annotations.find(classOf[CustomBase]) match {
      case Some(customBase) if customBase.value != "true" =>
        b.obj { m =>
          m.entry("$base", customBase.value)
        }
      case _ => // Nothing
    }
  }

  override def position(): Position = {
    annotations
      .flatMap(_.find(classOf[LexicalInformation]))
      .orElse(target.annotations.find(classOf[LexicalInformation]))
      .map(_.range.start)
      .getOrElse(ZERO)
  }
}
