package amf.plugins.document.vocabularies.parser.dialects

import amf.core.client.scala.model.domain.AmfScalar
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import amf.plugins.document.vocabularies.metamodel.domain.DocumentMappingModel
import amf.plugins.document.vocabularies.model.domain.DocumentMapping
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
