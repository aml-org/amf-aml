package amf.plugins.document.vocabularies.parser.dialects

import amf.core.model.domain.{AmfArray, AmfScalar}
import amf.core.parser.{Annotations, ScalarNode, SearchScope}
import amf.plugins.document.vocabularies.metamodel.domain.{DocumentMappingModel, DocumentsModelModel}
import amf.plugins.document.vocabularies.model.domain.{DocumentMapping, DocumentsModel}
import org.yaml.model.{YMap, YMapEntry, YScalar, YType}
import amf.core.utils._
import DialectAstOps._
import amf.plugins.features.validation.CoreValidations.DeclarationNotFound
case class DialectFragmentParser(into: DocumentsModel)(override implicit val ctx: DialectContext)
    extends DialectEntryParser {

  override def parse(entry: YMapEntry): Unit = {
    entry.value.as[YMap].parse("encodes", parseFragmentEncodesParser)
  }

  private val parseFragmentEncodesParser = new DialectEntryParser() {
    override def parse(entry: YMapEntry): Unit = {
      val docs = entry.value.tagType match {
        case YType.Map =>
          entry.value.as[YMap].entries.map { fragmentEntry =>
            val fragmentName = fragmentEntry.key.as[YScalar].text
            val nodeId       = fragmentEntry.value.as[YScalar].text
            val documentsMapping = DocumentMapping(fragmentEntry.value)
              .withDocumentName(fragmentName)
              .withId(into.id + s"/fragments/${fragmentName.urlComponentEncoded}")
            val nodeMapping = ctx.declarations.findNodeMappingOrError(entry.value)(nodeId, SearchScope.All)
            documentsMapping.set(DocumentMappingModel.EncodedNode,
              AmfScalar(nodeMapping.id, Annotations(entry.value)),
              Annotations(entry))
          }
        case _ =>
          ctx.eh.violation(DeclarationNotFound, "", s"NodeMappable ${entry.value} not found", entry)
          val documentMapping = DocumentMapping(entry.value)
          val nodeMapping = ctx.declarations.ErrorNodeMapabble(entry.key, entry.value)
          documentMapping.set(DocumentMappingModel.EncodedNode,
            AmfScalar(nodeMapping, Annotations(entry.value)),
            Annotations(entry))
          Seq(documentMapping)
      }
      into.set(DocumentsModelModel.Fragments, AmfArray(docs, Annotations(entry.value)), Annotations(entry))
    }
  }
}
