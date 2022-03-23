package amf.aml.internal.parse.dialects.nodemapping.like

import amf.aml.internal.parse.dialects.DialectContext
import amf.core.client.scala.model.domain.AmfScalar
import amf.core.internal.parser.domain.Annotations
import org.yaml.model.{YMapEntry, YSequence}

object MappingParsingHelper {

  def entrySeqNodesToString(entry: YMapEntry)(implicit ctx: DialectContext): IndexedSeq[AmfScalar] =
    entry.value.as[YSequence].nodes.map(n => AmfScalar(n.as[String], Annotations(n)))

}
