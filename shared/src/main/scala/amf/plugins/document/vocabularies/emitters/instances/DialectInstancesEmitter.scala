package amf.plugins.document.vocabularies.emitters.instances

import amf.core.annotations.Aliases
import amf.core.emitter.BaseEmitters._
import amf.core.emitter.SpecOrdering.Lexical
import amf.core.emitter.{RenderOptions, SpecOrdering}
import amf.core.model.document.{DeclaresModel, EncodesModel}
import amf.core.parser.Position
import amf.plugins.document.vocabularies.emitters.common.IdCounter
import amf.plugins.document.vocabularies.model.document._
import amf.plugins.document.vocabularies.model.domain._
import org.yaml.model.YDocument
import org.yaml.model.YDocument.PartBuilder

case class DialectInstancesEmitter(instance: DialectInstanceUnit, dialect: Dialect, renderOptions: RenderOptions)
    extends DialectEmitterHelper {
  val ordering: SpecOrdering                 = Lexical
  val aliases: Map[String, (String, String)] = collectAliases()

  def collectAliases(): Map[String, (String, String)] = {
    val vocabFile = instance.location().getOrElse(instance.id).split("/").last
    val vocabFilePrefix =
      instance.location().getOrElse(instance.id).replace(vocabFile, "")

    val maps = instance.annotations
      .find(classOf[Aliases])
      .map { aliases =>
        aliases.aliases.foldLeft(Map[String, String]()) {
          case (acc, (alias, (fullUrl, _))) =>
            acc + (fullUrl -> alias)
        }
      }
      .getOrElse(Map())
    val idCounter = new IdCounter()
    instance.references.foldLeft(Map[String, (String, String)]()) {
      case (acc: Map[String, (String, String)], m: DeclaresModel) =>
        val location = m.location().getOrElse(m.id).replace("#", "")
        val importLocation: String = if (location.contains(vocabFilePrefix)) {
          location.replace(vocabFilePrefix, "")
        }
        else {
          location.replace("file://", "")
        }

        if (maps.get(m.id).isDefined) {
          val alias = maps(m.id)
          acc + (m.id -> (alias, importLocation))
        }
        else {
          val nextAlias = idCounter.genId("uses_")
          acc + (m.id -> (nextAlias, importLocation))
        }
      case (acc: Map[String, (String, String)], _) => acc
    }
  }

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
        aliases,
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
