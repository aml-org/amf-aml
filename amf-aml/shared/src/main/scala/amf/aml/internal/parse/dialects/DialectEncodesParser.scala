package amf.aml.internal.parse.dialects

import amf.core.client.scala.model.domain.AmfScalar
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import amf.aml.internal.metamodel.domain.DocumentMappingModel
import amf.aml.client.scala.model.domain.DocumentMapping
import org.yaml.model.{YMapEntry, YScalar}

case class DialectEncodesParser(into: DocumentMapping)(override implicit val ctx: DialectContext)
    extends DialectEntryParser {
  override def parse(entry: YMapEntry): Unit = {
    val nodeId = entry.value.as[YScalar].text
    val nodeMapping = ctx.declarations
      .findNodeMappingOrError(entry.value)(nodeId, SearchScope.All)

    into.set(DocumentMappingModel.EncodedNode, AmfScalar(nodeMapping.id, Annotations(entry.value)), Annotations(entry))
  }
}
