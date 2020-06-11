package amf.plugins.document.vocabularies.parser.dialects

import amf.core.parser.Annotations
import amf.plugins.document.vocabularies.model.domain.DocumentsModel
import amf.plugins.document.vocabularies.parser.dialects.DialectAstOps._
import org.yaml.model.YMap
// todo: should depend of SpecParserOps??? move to core??
case class DocumentsModelParser(map: YMap, parentId: String, name: String)(implicit val ctx: DialectContext) {

  def parse(): DocumentsModel = {
    val documentsMapping: DocumentsModel = DocumentsModel(Annotations(map)).withId(parentId + "#/documents")
    ctx.closedNode("documentsMapping", documentsMapping.id, map)

    map.parse("root", RootDocumentParser(documentsMapping, name))
    map.parse("fragments", DialectFragmentParser(documentsMapping))
    map.parse("library", DialectLibrariesParser(documentsMapping, name))
    map.parse("options", DocumentOptionsParser(documentsMapping))
    documentsMapping
  }
}
