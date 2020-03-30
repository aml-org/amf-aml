package amf.plugins.document.vocabularies.parser.dialects

import amf.core.model.domain.AmfScalar
import amf.core.parser.{Annotations, SearchScope}
import amf.plugins.document.vocabularies.metamodel.domain.{DocumentMappingModel, DocumentsModelModel}
import amf.plugins.document.vocabularies.model.domain.DocumentMapping
import org.yaml.model.{YMapEntry, YScalar}


case class DialectEncodesParser(into:DocumentMapping)(override implicit val ctx:DialectContext) extends DialectEntryParser {
  override def parse(entry:YMapEntry): Unit = {
    val nodeId = entry.value.as[YScalar].text
    ctx.declarations
      .findNodeMapping(nodeId, SearchScope.All)
      .foreach(nodeMapping =>
        into.set(DocumentMappingModel.EncodedNode,AmfScalar(nodeMapping.id,Annotations(entry.value)), Annotations(entry)))
  }
}
