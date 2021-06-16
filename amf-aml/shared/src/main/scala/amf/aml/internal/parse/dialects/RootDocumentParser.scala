package amf.aml.internal.parse.dialects

import amf.core.internal.parser.domain.Annotations
import amf.aml.internal.metamodel.domain.DocumentsModelModel
import amf.aml.client.scala.model.domain.{DocumentMapping, DocumentsModel}
import org.yaml.model.{YMap, YMapEntry}
import DialectAstOps._

case class RootDocumentParser(into: DocumentsModel, name: String)(override implicit val ctx: DialectContext)
    extends DialectEntryParser {

  override def parse(entry: YMapEntry): Unit = {
    val rootMap          = entry.value.as[YMap]
    val documentsMapping = DocumentMapping(Annotations(entry.value)).withDocumentName(name).withId(into.id + "/root")
    rootMap.parse("encodes", DialectEncodesParser(documentsMapping))
    DialectDeclaresParser(documentsMapping).parse(rootMap)

    into.set(DocumentsModelModel.Root, documentsMapping, Annotations(entry))
  }
}
