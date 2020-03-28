package amf.plugins.document.vocabularies.parser.dialects

import amf.core.model.domain.{AmfArray, AmfScalar}
import amf.core.parser.{Annotations, ScalarNode, SearchScope}
import amf.plugins.document.vocabularies.metamodel.domain.{DocumentMappingModel, DocumentsModelModel}
import amf.plugins.document.vocabularies.model.domain.{DocumentMapping, DocumentsModel}
import org.yaml.model.{YMap, YMapEntry, YScalar}
import amf.core.utils._
import DialectAstOps._
case class DialectFragmentParser(into:DocumentsModel)(override implicit val ctx:DialectContext) extends DialectEntryParser {

  override def parse(entry:YMapEntry):Unit = {
    entry.value.as[YMap].parse("encodes", parseFragmentEncodesParser)
  }


  private val parseFragmentEncodesParser = new DialectEntryParser() {
    override def parse(entry: YMapEntry): Unit = {
      val docs = entry.value.as[YMap].entries.flatMap { fragmentEntry =>
        val fragmentName = fragmentEntry.key.as[YScalar].text
        val nodeId       = fragmentEntry.value.as[YScalar].text
        val documentsMapping = DocumentMapping(fragmentEntry.value)
          .withDocumentName(fragmentName)
          .withId(into.id + s"/fragments/${fragmentName.urlComponentEncoded}")
        ctx.declarations.findNodeMapping(nodeId, SearchScope.All) match {
          case Some(nodeMapping) => Some(documentsMapping.withEncoded(nodeMapping.id))
          case _ =>
            ctx.missingTermViolation(nodeId, into.id, fragmentEntry)
            None
        }
      }
      into.set(DocumentsModelModel.Fragments, AmfArray(docs, Annotations(entry.value)), Annotations(entry))
    }
  }
}
