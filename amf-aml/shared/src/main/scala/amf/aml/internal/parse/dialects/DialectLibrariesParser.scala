package amf.aml.internal.parse.dialects

import amf.core.internal.parser.domain.Annotations
import amf.aml.internal.metamodel.domain.DocumentsModelModel
import amf.aml.client.scala.model.domain.{DocumentMapping, DocumentsModel}
import org.yaml.model.{YMap, YMapEntry}

case class DialectLibrariesParser(into: DocumentsModel, dialectName: String)(override implicit val ctx: DialectContext)
    extends DialectEntryParser {
  private val name = s"$dialectName / Library"

  override def parse(entry: YMapEntry): Unit = {
    val documentsMapping = DocumentMapping(Annotations(entry)).withDocumentName(name).withId(into.id + "/modules")
    DialectDeclaresParser(documentsMapping).parse(entry.value.as[YMap])
    into.set(DocumentsModelModel.Library, documentsMapping, Annotations(entry))
  }
}
