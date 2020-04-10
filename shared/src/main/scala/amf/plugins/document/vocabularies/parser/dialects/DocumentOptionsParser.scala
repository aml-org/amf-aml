package amf.plugins.document.vocabularies.parser.dialects

import amf.core.model.domain.AmfScalar
import org.yaml.model.{YMap, YMapEntry, YType}
import amf.core.parser._
import amf.plugins.document.vocabularies.metamodel.domain.DocumentsModelModel
import amf.plugins.document.vocabularies.model.domain.DocumentsModel
import amf.validation.DialectValidations.DialectError
import DialectAstOps._
import amf.plugins.document.vocabularies.ReferenceStyles
import amf.plugins.document.vocabularies.metamodel.domain
case class DocumentOptionsParser(into:DocumentsModel)(override implicit val ctx:DialectContext) extends DialectEntryParser{

  override def parse(entry: YMapEntry): Unit = {
    entry.value.toOption[YMap] match {
      case Some(optionsMap) =>
        ctx.closedNode("documentsMappingOptions", into.id, optionsMap)
        parseOptions(optionsMap)
      case _ =>
        ctx.eh.violation(DialectError,
          into.id,
          "Options for a documents mapping must be a map",
          entry.value)
    }
  }

  private def parseOptions(map: YMap): Unit = {
    map.parse("selfEncoded", into setParsing DocumentsModelModel.SelfEncoded)
    map.parse("declarationsPath", into setParsing DocumentsModelModel.DeclarationsPath)
    map.parse("keyProperty", into setParsing DocumentsModelModel.KeyProperty)
    map.parse("referenceStyle", into setParsing DocumentsModelModel.ReferenceStyle)
  }
}
