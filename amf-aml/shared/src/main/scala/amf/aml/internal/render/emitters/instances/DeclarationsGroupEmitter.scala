package amf.aml.internal.render.emitters.instances
import amf.aml.client.scala.model.document.{Dialect, DialectInstanceUnit}
import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMappable, PublicNodeMapping, UnionNodeMapping}
import amf.aml.internal.metamodel.domain.NodeMappableModel
import amf.aml.internal.registries.AMLRegistry
import amf.core.client.scala.config.RenderOptions
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.core.internal.utils.AmfStrings
import org.mulesoft.common.client.lexical.Position
import org.mulesoft.common.client.lexical.Position.ZERO
import org.yaml.model.YDocument.EntryBuilder
import org.yaml.model.YNode

case class DeclarationsGroupEmitter(
    declared: Seq[DialectDomainElement],
    publicNodeMapping: PublicNodeMapping,
    nodeMappable: NodeMappable[_ <: NodeMappableModel],
    instance: DialectInstanceUnit,
    dialect: Dialect,
    ordering: SpecOrdering,
    declarationsPath: Seq[String],
    aliases: Map[String, (String, String)],
    keyPropertyId: Option[String] = None,
    renderOptions: RenderOptions,
    registry: AMLRegistry
)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends EntryEmitter
    with AmlEmittersHelper {

  def computeIdentifier(decl: DialectDomainElement): String = {
    decl.declarationName.option() match {
      case Some(name) => name
      case _ =>
        decl.id
          .split("#")
          .last
          .split("/")
          .last
          .urlDecoded // we are using the last part of the URL as the identifier in dialects
    }
  }

  override def emit(b: EntryBuilder): Unit = {
    val discriminator = findNodeMappingById(publicNodeMapping.mappedNode().value()) match {
      case (_, unionMapping: UnionNodeMapping) =>
        Some(DiscriminatorHelper(unionMapping, this))
      case _ => None
    }

    if (declarationsPath.isEmpty) {
      val declarationKey = publicNodeMapping.name().value()
      b.entry(
          declarationKey,
          _.obj { b =>
            sortedDeclarations().foreach { decl =>
              val identifier = computeIdentifier(decl)
              b.entry(
                  YNode(identifier),
                  b => {
                    val discriminatorProperty =
                      discriminator.flatMap(_.compute(decl))
                    DialectNodeEmitter(
                        decl,
                        nodeMappable,
                        instance.references,
                        dialect,
                        ordering,
                        discriminator = discriminatorProperty,
                        renderOptions = renderOptions,
                        registry = registry
                    ).emit(b)
                  }
              )
            }
          }
      )
    } else {
      b.entry(
          declarationsPath.head,
          _.obj { b =>
            DeclarationsGroupEmitter(
                declared,
                publicNodeMapping,
                nodeMappable,
                instance,
                dialect,
                ordering,
                declarationsPath.tail,
                aliases,
                keyPropertyId,
                renderOptions,
                registry
            ).emit(b)
          }
      )
    }
  }

  override def position(): Position =
    declared
      .flatMap(_.annotations.find(classOf[LexicalInformation]).map { lexInfo =>
        lexInfo.range.start
      })
      .sorted
      .headOption
      .getOrElse(ZERO)

  def sortedDeclarations(): Seq[DialectDomainElement] = {
    declared.sortBy(
        _.annotations
          .find(classOf[LexicalInformation])
          .map { lexInfo =>
            lexInfo.range.start
          }
          .getOrElse(ZERO)
    )
  }
}
