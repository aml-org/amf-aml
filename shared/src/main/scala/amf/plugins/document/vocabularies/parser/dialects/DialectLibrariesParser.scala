package amf.plugins.document.vocabularies.parser.dialects

import amf.core.parser.Annotations
import amf.plugins.document.vocabularies.metamodel.domain.DocumentsModelModel
import amf.plugins.document.vocabularies.model.domain.{DocumentMapping, DocumentsModel}
import org.yaml.model.{YMap, YMapEntry}

case class DialectLibrariesParser(into:DocumentsModel, dialectName:String)(override implicit val ctx:DialectContext) extends DialectEntryParser {
  private val name             = s"$dialectName / Library"

  override def parse(entry:YMapEntry): Unit = {
      val documentsMapping = DocumentMapping(Annotations(entry)).withDocumentName(name).withId(into.id + "/modules")
      DialectDeclaresParser(documentsMapping).parse(entry.value.as[YMap])
      into.set(DocumentsModelModel.Library, documentsMapping, Annotations(entry))
  }
}
