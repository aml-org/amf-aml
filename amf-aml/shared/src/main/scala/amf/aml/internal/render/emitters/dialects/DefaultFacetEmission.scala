package amf.aml.internal.render.emitters.dialects

import amf.aml.client.scala.model.domain.WithDefaultFacet
import amf.core.client.scala.errorhandling.IgnoringErrorHandler
import amf.core.internal.datanode.DataNodeEmitter
import amf.core.internal.metamodel.domain.ShapeModel
import amf.core.internal.render.BaseEmitters.EntryPartEmitter
import amf.core.internal.render.SpecOrdering
import amf.core.internal.render.emitters.EntryEmitter

trait DefaultFacetEmission extends PosExtractor {
  protected def emitDefault(mapping: WithDefaultFacet): List[EntryEmitter] = {
    mapping
      .default()
      .map { dataNode =>
        EntryPartEmitter("default",
                         DataNodeEmitter(dataNode, SpecOrdering.Lexical)(IgnoringErrorHandler),
                         position = fieldPos(mapping, ShapeModel.Default))
      }
      .toList
  }
}
