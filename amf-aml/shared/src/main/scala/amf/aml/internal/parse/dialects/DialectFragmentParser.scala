package amf.aml.internal.parse.dialects

import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.internal.parser.domain.{Annotations, SearchScope}
import amf.core.internal.utils._
import amf.core.internal.validation.CoreValidations.DeclarationNotFound
import amf.aml.internal.metamodel.domain.{DocumentMappingModel, DocumentsModelModel}
import amf.aml.client.scala.model.domain.{DocumentMapping, DocumentsModel}
import amf.aml.internal.parse.dialects.DialectAstOps._
import org.yaml.model.{YMap, YMapEntry, YScalar, YType}
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
          ctx.eh.violation(DeclarationNotFound, "", s"NodeMappable ${entry.value} not found", entry.location)
          val documentMapping = DocumentMapping(entry.value)
          val nodeMapping     = ctx.declarations.ErrorNodeMappable(entry.key, entry.value)
          documentMapping.set(DocumentMappingModel.EncodedNode,
                              AmfScalar(nodeMapping, Annotations(entry.value)),
                              Annotations(entry))
          Seq(documentMapping)
      }
      into.set(DocumentsModelModel.Fragments, AmfArray(docs, Annotations(entry.value)), Annotations(entry))
    }
  }
}
