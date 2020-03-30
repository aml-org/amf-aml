package amf.plugins.document.vocabularies.parser.dialects

import amf.core.model.domain.AmfScalar
import org.yaml.model.{YMap, YMapEntry, YType}
import amf.core.parser._
import amf.plugins.document.vocabularies.metamodel.domain.DocumentsModelModel
import amf.plugins.document.vocabularies.model.domain.DocumentsModel
import amf.validation.DialectValidations.DialectError
import DialectAstOps._
import amf.plugins.document.vocabularies.ReferenceStyles
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
    map.parse("selfEncoded", selfEncodedParser)
    map.parse("declarationsPath", declarationsPathParser)
    map.parse("keyProperty", keyPropertyParser)
    map.parse("referenceStyle", referenceStyleParser)
  }

  private val selfEncodedParser = new DialectEntryParser {
    override def parse(entry: YMapEntry): Unit = {
      entry.value.tagType match {
        case YType.Bool =>
          val selfEncoded: AmfScalar = ScalarNode(entry.value).boolean()
          into.set(DocumentsModelModel.SelfEncoded, selfEncoded, Annotations(entry))
        case _ =>
          ctx.eh.violation(DialectError,
            into.id,
            "'selfEncoded' Option for a documents mapping must be a boolean",
            entry)
      }
    }
  }

  private val keyPropertyParser = new DialectEntryParser {
    override def parse(entry: YMapEntry): Unit = {
      entry.value.tagType match {
        case YType.Bool =>
          val keyProperty = ScalarNode(entry.value).boolean()
          into.set(DocumentsModelModel.KeyProperty, keyProperty, Annotations(entry))
        case _ =>
          ctx.eh.violation(DialectError,
            into.id,
            "'keyProperty' Option for a documents mapping must be a boolean",
            entry)
      }
    }
  }

  private val declarationsPathParser = new DialectEntryParser {
    override def parse(entry: YMapEntry): Unit = {
      entry.value.tagType match {
        case YType.Str =>
          into.set(DocumentsModelModel.DeclarationsPath, ScalarNode(entry.value).string(), Annotations(entry))
        case _ =>
          ctx.eh.violation(DialectError,
            into.id,
            "'declarationsPath' Option for a documents mapping must be a String",
            entry)
      }
    }
  }

  private val referenceStyleParser =  new DialectEntryParser {
    override def parse(entry: YMapEntry): Unit = {
      entry.value.tagType match {
        case YType.Str if ReferenceStyles.all.contains(entry.value.asScalar.map(_.text).getOrElse("")) =>
          into.set(DocumentsModelModel.ReferenceStyle, ScalarNode(entry.value).string(), Annotations(entry))
        case _ =>
          ctx.eh.violation(DialectError,
            into.id,
            "'referenceStyle' Option for a documents mapping must be a String [RamlStyle, JsonSchemaStyle]",
            entry)
      }
    }
  }
}
