package amf.aml.internal.parse.dialects

import amf.core.internal.parser.domain.Annotations
import amf.core.internal.validation.CoreValidations.SyamlError
import amf.aml.client.scala.model.domain.DocumentsModel
import amf.aml.internal.parse.dialects.DialectAstOps._
import org.yaml.model.{YMap, YNode, YType}
// todo: should depend of SpecParserOps??? move to core??
case class DocumentsModelParser(node: YNode, parentId: String, name: String)(implicit val ctx: DialectContext) {
  def parse(): DocumentsModel =
    node.tagType match {
      case YType.Map => parseMap(node.as[YMap])
      case _ =>
        ctx.eh.violation(SyamlError, "", s"Map expected inside documents, found [${node.tagType}]", node.location)
        noMap
    }

  private def parseMap(map: YMap): DocumentsModel = {
    val documentsMapping: DocumentsModel = DocumentsModel(Annotations(map)).withId(parentId + "#/documents")
    ctx.closedNode("documentsMapping", documentsMapping.id, map)

    map.parse("root", RootDocumentParser(documentsMapping, name))
    map.parse("fragments", DialectFragmentParser(documentsMapping))
    map.parse("library", DialectLibrariesParser(documentsMapping, name))
    map.parse("options", DocumentOptionsParser(documentsMapping))
    documentsMapping
  }

  private def noMap: DocumentsModel =
    DocumentsModel(Annotations(node)).withId(parentId + "#/documents") // in order to keep AST
}
