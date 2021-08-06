package amf.aml.internal.render.emitters.instances
import amf.aml.client.scala.AMLConfiguration
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.core.client.common.position.Position
import amf.core.internal.annotations.Aliases.{Alias, FullUrl, ImportLocation, RefId}
import amf.core.internal.annotations.{Aliases, LexicalInformation}
import amf.core.internal.render.BaseEmitters.traverse
import amf.core.internal.render.SpecOrdering
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.parse.document.ParserContext
import amf.core.internal.render.emitters.EntryEmitter
import amf.aml.internal.render.emitters.common.{ExternalEmitter, IdCounter}
import amf.aml.client.scala.model.document.{Dialect, DialectLibrary, ExternalContext}
import amf.aml.client.scala.model.domain.{NodeMappable, NodeMapping, UnionNodeMapping}
import org.yaml.model.YDocument.EntryBuilder
import amf.core.internal.utils.Regexes.Path
import amf.aml.client.scala.model.domain.NodeMappable.AnyNodeMappable
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances.identityMonad

import scala.collection.mutable

trait NodeMappableFinder {
  def findNode(id: String): Option[(Dialect, AnyNodeMappable)]
}

object DefaultNodeMappableFinder {
  def apply(dialects: Seq[Dialect]) = new DefaultNodeMappableFinder(dialects)
  def apply(ctx: ParserContext) = {
    val knownDialects = ctx.config.sortedParsePlugins.collect {
      case plugin: AMLDialectInstanceParsingPlugin => plugin.dialect
    }
    new DefaultNodeMappableFinder(knownDialects)
  }
  def apply(config: AMLConfiguration) = {
    val knownDialects = config.configurationState().getDialects()
    new DefaultNodeMappableFinder(knownDialects)
  }
  def empty() = new DefaultNodeMappableFinder(Seq.empty)
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
          case (alias, (fullUrl, _)) =>
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
  val nodeMappingCache: mutable.HashMap[NodeMappingId, (Dialect, NodeMappable)] = mutable.HashMap.empty

  def findNodeMappingById(nodeMappingId: NodeMappingId): (Dialect, NodeMappable) = {
    nodeMappingCache
      .get(nodeMappingId)
      .orElse(maybeFindNodeMappingById(nodeMappingId)) match {
      case Some(result) =>
        nodeMappingCache(nodeMappingId) = result
        result
      case None =>
        throw new Exception(s"Cannot find node mapping $nodeMappingId")
    }
  }

  protected def findAllNodeMappings(mappableId: String): Seq[NodeMapping] = {
    findNodeMappingById(mappableId) match {
      case (_, nodeMapping: NodeMapping) => Seq(nodeMapping)
      case (_, unionMapping: UnionNodeMapping) =>
        val mappables = unionMapping.objectRange() map { rangeId =>
          findNodeMappingById(rangeId.value())._2
        }
        mappables.collect { case nodeMapping: NodeMapping => nodeMapping }
      case _ => Nil
    }
  }

  def maybeFindNodeMappingById(nodeMappingId: NodeMappingId): Option[(Dialect, NodeMappable)] = {
    val inDialectMapping = dialect.declares
      .find { element =>
        element.id == nodeMappingId
      }
      .map { nodeMapping =>
        (dialect, nodeMapping)
      }
      .asInstanceOf[Option[(Dialect, NodeMappable)]]
      .orElse {
        dialect.references
          .collect {
            case lib: DialectLibrary =>
              lib.declares.find(_.id == nodeMappingId)
          }
          .collectFirst {
            case Some(mapping: NodeMappable) =>
              (dialect, mapping)
          }
      }
    inDialectMapping orElse {
      findNodeInRegistry(nodeMappingId)
    }
  }

  def findNodeInRegistry(nodeMappingId: String): Option[(Dialect, NodeMappable)] =
    nodeMappableFinder.findNode(nodeMappingId)
}
