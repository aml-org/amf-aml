package amf.plugins.document.vocabularies.emitters.instances
import amf.core.annotations.LexicalInformation
import amf.core.emitter.BaseEmitters.traverse
import amf.core.emitter.{EntryEmitter, SpecOrdering}
import amf.core.model.domain.AmfObject
import amf.core.parser.Position
import amf.core.parser.Position.ZERO
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.emitters.common.ExternalEmitter
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectLibrary, ExternalContext}
import amf.plugins.document.vocabularies.model.domain.NodeMappable
import org.yaml.model.YDocument.EntryBuilder

import scala.collection.mutable

trait DialectEmitterHelper {
  val dialect: Dialect

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

  def maybeFindNodeMappingById(nodeMappingId: NodeMappingId): Option[(Dialect, NodeMappable)] = {
    val inDialectMapping = dialect.declares
      .find {
        case nodeMapping: NodeMappable => nodeMapping.id == nodeMappingId
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
    AMLPlugin().registry.findNode(nodeMappingId)
}
