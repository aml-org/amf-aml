package amf.plugins.document.vocabularies.emitters.instances

import amf.core.client.common.position.Position
import amf.core.internal.annotations.Aliases.{Alias, ImportLocation, RefId}
import amf.core.internal.render.BaseEmitters._
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.EncodesModel
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.SpecOrdering.Lexical
import amf.plugins.document.vocabularies.model.document._
import amf.plugins.document.vocabularies.model.domain._
import org.yaml.model.YDocument
import org.yaml.model.YDocument.PartBuilder

case class DialectInstancesEmitter(instance: DialectInstanceUnit, dialect: Dialect, renderOptions: RenderOptions)(
    implicit val nodeMappableFinder: NodeMappableFinder)
    extends AmlEmittersHelper {

  val ordering: SpecOrdering                           = Lexical
  val references: Map[RefKey, (Alias, ImportLocation)] = buildReferenceAliasIndexFrom(instance)

  override protected def sanitize(importLocation: ImportLocation): ImportLocation =
    importLocation.replace("#", "")

  def emitInstance(): YDocument = {
    YDocument(b => {
      instance match {
        case unit: EncodesModel => emitEncoded(b, unit)
        case _                  =>
      }
    })
  }

  private def emitEncoded(b: PartBuilder, encoded: EncodesModel): Unit = {

    val schema = s"${dialect.name().value()} ${dialect.version().value()}"

    val (entry, root) = encoded match {
      case _: DialectInstance =>
        val r = dialect.documents().root().encoded().value()
        if (dialect.documents().keyProperty().value()) (headerEntry(), r)
        else {
          b.comment(s"%$schema")
          (Nil, r)
        }
      case f: DialectInstanceFragment =>
        b.comment(s"%${f.fragment()} / $schema")
        (Nil,
         dialect.documents().fragments().find(_.documentName().is(f.fragment().value())).map(_.encoded().value()).get)
    }

    val (_, rootNodeMapping) = findNodeMappingById(root)

    val discriminator = rootNodeMapping match {
      case mapping: UnionNodeMapping => Some(DiscriminatorHelper(mapping, this))
      case _                         => None
    }

    val element = encoded.encodes.asInstanceOf[DialectDomainElement]

    new RootDialectNodeEmitter(
        element,
        rootNodeMapping,
        instance,
        dialect,
        ordering,
        None,
        topLevelEmitters = externalEmitters(instance, ordering) ++ entry,
        discriminator = discriminator.flatMap(_.compute(element)),
        renderOptions = renderOptions
    ).emit(b)
  }

  /** Return dialect name and version as entry. */
  private def headerEntry() = {
    Seq(MapEntryEmitter(dialect.name().value(), dialect.version().value(), position = Position.FIRST))
  }
}
