package amf.plugins.document.vocabularies.emitters.instances

import amf.core.annotations.Aliases.{Alias, ImportLocation, RefId}
import amf.core.emitter.BaseEmitters._
import amf.core.emitter.SpecOrdering.Lexical
import amf.core.emitter.{RenderOptions, SpecOrdering}
import amf.core.model.document.{DeclaresModel, EncodesModel}
import amf.core.parser.Position
import amf.plugins.document.vocabularies.model.document._
import amf.plugins.document.vocabularies.model.domain._
import org.yaml.model.YDocument
import org.yaml.model.YDocument.PartBuilder

case class DialectInstancesEmitter(instance: DialectInstanceUnit, dialect: Dialect, renderOptions: RenderOptions)
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

    DialectNodeEmitter(
        element,
        rootNodeMapping,
        instance,
        dialect,
        ordering,
        None,
        rootNode = true,
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
