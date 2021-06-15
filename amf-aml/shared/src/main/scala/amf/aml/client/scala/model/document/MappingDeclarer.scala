package amf.aml.client.scala.model.document

import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.aml.client.scala.model.domain.NodeMapping

trait MappingDeclarer { this: BaseUnit with DeclaresModel =>

  def findNodeMapping(mappingId: String): Option[NodeMapping] = {
    declares.find(_.id == mappingId) match {
      case Some(mapping: NodeMapping) => Some(mapping)
      case _ =>
        references
          .collect {
            case lib: MappingDeclarer =>
              lib
          }
          .map { dec =>
            dec.findNodeMapping(mappingId)
          }
          .filter(_.isDefined)
          .map(_.get)
          .headOption
    }
  }
}
