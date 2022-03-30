package amf.aml.internal.render.emitters.instances
import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.document.{Dialect, ExternalContext}
import amf.aml.client.scala.model.domain.NodeMappable.AnyNodeMappable
import amf.aml.client.scala.model.domain.NodeMapping
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.aml.internal.render.emitters.common.{ExternalEmitter, IdCounter}
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.parse.document.ParserContext
import amf.core.internal.annotations.Aliases.{Alias, FullUrl, ImportLocation}
import amf.core.internal.annotations.{Aliases, LexicalInformation, ReferencedInfo}
import amf.core.internal.render.BaseEmitters.traverse
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter
import amf.core.internal.utils.Regexes.Path
import org.mulesoft.common.collections.FilterType
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances.identityMonad
import org.yaml.model.YDocument.EntryBuilder

import scala.collection.mutable

trait NodeMappableFinder {
  def findNode(id: String): Option[(Dialect, AnyNodeMappable)]
}

object DefaultNodeMappableFinder {
  def apply(dialect: Dialect)       = new DefaultNodeMappableFinder(computeReferencesTree(dialect))
  def apply(dialects: Seq[Dialect]) = new DefaultNodeMappableFinder(dialects)
  def apply(ctx: ParserContext) = {
    val knownDialects = ctx.config.sortedRootParsePlugins.collect {
      case plugin: AMLDialectInstanceParsingPlugin => plugin.dialect
    }
    new DefaultNodeMappableFinder(knownDialects)
  }

  def apply(config: AMLConfiguration) = {
    val knownDialects = config.configurationState().getDialects()
    new DefaultNodeMappableFinder(knownDialects)
  }

  def empty() = new DefaultNodeMappableFinder(Seq.empty)

  private def computeReferencesTree(from: Dialect): List[Dialect] = {
    val collector = mutable.Map[String, Dialect]()
    computeReferencesTree(from, collector)
    collector.values.toList
  }

  private def computeReferencesTree(from: Dialect, acc: mutable.Map[String, Dialect]): Unit = {
    acc.put(from.id, from)
    from.references
      .filterType[Dialect]
      .filter(dialect => !acc.contains(dialect.id))
      .foreach(dialect => computeReferencesTree(dialect, acc))
  }
}

case class DefaultNodeMappableFinder(dialects: Seq[Dialect]) extends NodeMappableFinder {

  private val mappableQuery = CachedFunction.from[String, Option[(Dialect, AnyNodeMappable)]] { nodeMappableId =>
    dialects
      .find(dialect => nodeMappableId.contains(dialect.id))
      .map { dialect =>
        (dialect, dialect.declares.find(_.id == nodeMappableId))
      }
      .collectFirst {
        case (dialect, Some(nodeMapping: NodeMapping)) => (dialect, nodeMapping)
      }
  }

  override def findNode(id: String): Option[(Dialect, AnyNodeMappable)] = mappableQuery.runCached(id)

  def addDialect(dialect: Dialect): DefaultNodeMappableFinder = copy(dialects :+ dialect)
}

trait DialectEmitterHelper {

  type RefKey       = String
  type NodeMappable = AnyNodeMappable

  protected def buildReferenceAliasIndexFrom(unit: BaseUnit): Map[RefKey, (Alias, ImportLocation)] = {
    val aliases   = extractAliasesFrom(unit)
    val idCounter = new IdCounter()
    unit.references.toStream
      .filter(_.isInstanceOf[DeclaresModel])
      .map {
        case m: DeclaresModel =>
          val key            = referenceIndexKeyFor(m)
          val importLocation = getImportLocation(unit, m)
          aliases.get(key) match {
            case Some(alias) =>
              key -> (alias, importLocation)
            case None =>
              val generatedAlias = idCounter.genId("uses")
              key -> (generatedAlias, importLocation)
          }
      }
      .toMap
  }

  protected def extractAliasesFrom(unit: BaseUnit): Map[FullUrl, Alias] = {
    unit.annotations
      .find(classOf[Aliases])
      .map { aliasesAnnotation =>
        aliasesAnnotation.aliases.map {
          case (alias, ReferencedInfo(_, fullUrl, _)) =>
            fullUrl -> alias
        }.toMap
      }
      .getOrElse(Map.empty)
  }

  // Override if necessary
  protected def referenceIndexKeyFor(unit: DeclaresModel): RefKey = unit.id

  protected def getImportLocation(unit: BaseUnit, reference: BaseUnit): String = {
    unit.location().getOrElse(unit.id) match {
      case Path(parent, _) =>
        val location = sanitize {
          reference
            .location()
            .getOrElse(reference.id)
        }

        if (location.contains(parent)) {
          location.replace(s"$parent/", "")
        } else {
          location.replace("file://", "")
        }
    }
  }

  protected def sanitize(importLocation: ImportLocation): ImportLocation = importLocation
}

trait AmlEmittersHelper extends DialectEmitterHelper {

  val dialect: Dialect
  implicit val nodeMappableFinder: NodeMappableFinder

  def externalEmitters[T <: AmfObject](model: ExternalContext[T], ordering: SpecOrdering): Seq[EntryEmitter] = {
    if (model.externals.nonEmpty) {
      Seq(new EntryEmitter {
        override def emit(b: EntryBuilder): Unit = {
          b.entry("$external", _.obj({ b =>
            traverse(ordering.sorted(model.externals.map(external => ExternalEmitter(external, ordering))), b)
          }))
        }

        override def position(): Position = {
          model.externals
            .map(
                e =>
                  e.annotations
                    .find(classOf[LexicalInformation])
                    .map(_.range.start))
            .filter(_.nonEmpty)
            .map(_.get)
            .sortBy(_.line)
            .headOption
            .getOrElse(ZERO)
        }
      })
    } else {
      Nil
    }
  }

  type NodeMappingId = String
  private val index = DialectIndex(dialect, nodeMappableFinder)

  def findAllNodeMappings(mappableId: String): Seq[NodeMapping] = index.findAllNodeMappings(mappableId)
  def findNodeMappingById(nodeMappingId: NodeMappingId): (Dialect, AnyNodeMappable) =
    index.findNodeMappingById(nodeMappingId)
  def maybeFindNodeMappingById(nodeMappingId: NodeMappingId): Option[(Dialect, AnyNodeMappable)] =
    index.maybeFindNodeMappingById(nodeMappingId)
}
