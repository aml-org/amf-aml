package amf.aml.internal.parse.dialects

import amf.core.client.scala.model.domain.AmfScalar
import org.yaml.model.{YMap, YMapEntry, YType}
import amf.core.internal.parser._
import amf.aml.internal.metamodel.domain.DocumentsModelModel
import amf.aml.client.scala.model.domain.DocumentsModel
import amf.aml.internal.validate.DialectValidations.DialectError
import DialectAstOps._
import amf.aml.internal.metamodel.domain
case class DocumentOptionsParser(into: DocumentsModel)(override implicit val ctx: DialectContext)
    extends DialectEntryParser {

  override def parse(entry: YMapEntry): Unit = {
    entry.value.toOption[YMap] match {
      case Some(optionsMap) =>
        ctx.closedNode("documentsMappingOptions", into.id, optionsMap)
        parseOptions(optionsMap)
      case _ =>
        ctx.eh.violation(DialectError, into.id, "Options for a documents mapping must be a map", entry.value.location)
    }
  }

  private def parseOptions(map: YMap): Unit = {
    map.parse("selfEncoded", into setParsing DocumentsModelModel.SelfEncoded)
    map.parse("declarationsPath", into setParsing DocumentsModelModel.DeclarationsPath)
    map.parse("keyProperty", into setParsing DocumentsModelModel.KeyProperty)
    map.parse("referenceStyle", into setParsing DocumentsModelModel.ReferenceStyle)
  }
}
