package amf.aml.internal.parse.common

import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.parse.document.{
  CompilerReferenceCollector,
  LibraryReference,
  LinkReference,
  ParsedDocument,
  ParserContext,
  ReferenceHandler,
  ReferenceKind,
  SchemaReference,
  SyamlParsedDocument
}
import amf.core.internal.parser.YMapOps
import amf.core.internal.validation.CoreValidations.InvalidInclude
import amf.aml.client.scala.model.document.Dialect
import amf.aml.internal.validate.DialectValidations.InvalidModuleType
import org.yaml.model._

class SyntaxExtensionsReferenceHandler(eh: AMFErrorHandler) extends ReferenceHandler {
  private val collector = CompilerReferenceCollector()

  override def collect(parsedDoc: ParsedDocument, ctx: ParserContext): CompilerReferenceCollector = {
    val dialects = ctx.config.sortedParsePlugins.collect {
      case plugin: AMLDialectInstanceParsingPlugin => plugin.dialect
    }
    collect(parsedDoc, dialects)(ctx.eh)
  }

  private def collect(parsedDoc: ParsedDocument, dialects: Seq[Dialect])(implicit errorHandler: AMFErrorHandler) = {
    parsedDoc match {
      case parsed: SyamlParsedDocument =>
        parsed.comment.filter(referencesDialect).foreach { comment =>
          collector += (dialectDefinitionUrl(comment), SchemaReference, parsed.document.node)
        }
        libraries(parsed.document)
        links(parsed.document)

      case _ => // ignore
    }

    collector
  }

  def dialectDefinitionUrl(mt: String): String = {
    val io = mt.indexOf("|")
    if (io > 0) {
      val msk = mt.substring(io + 1)
      val si  = msk.indexOf("<")
      val se  = msk.lastIndexOf(">")
      msk.substring(si + 1, se)
    } else
      ""
  }

  private def libraries(document: YDocument)(implicit errorHandler: AMFErrorHandler): Unit = {
    document.to[YMap] match {
      case Right(map) =>
        map
          .key("uses")
          .foreach { entry =>
            entry.value.to[YMap] match {
              case Right(m) => m.entries.foreach(library)
              case _ =>
                errorHandler.violation(InvalidModuleType, "", s"Expected map but found: ${entry.value}", entry.value)
            }
          }
      case _ =>
    }
  }

  private def referencesDialect(mt: String): Boolean = {
    val io = mt.indexOf("|")
    if (io > 0) {
      val msk = mt.substring(io + 1)
      val si  = msk.indexOf("<")
      val se  = msk.lastIndexOf(">")
      si > 0 && se > si
    } else
      false
  }

  private def library(entry: YMapEntry)(implicit errorHandler: AMFErrorHandler): Unit = {
    collector += (entry.value, LibraryReference, entry.value)
  }

  private def links(part: YPart)(implicit errorHandler: AMFErrorHandler): Unit = {
    part match {
      case entry: YMapEntry =>
        entry.key.as[YScalar].text match {
          case "$target" => // patch $target link
            val includeRef = entry.value
            ramlInclude(includeRef)

          case "$include" => // !include as $include link
            val includeRef = entry.value
            ramlInclude(includeRef)

          case "$ref" => // $ref link
            entry.value.asScalar.map(_.text).foreach(ramlIncludeText)

          case "$dialect" => // reference to nested dialect
            entry.value.asScalar.map(_.text).foreach(ramlIncludeText)

          case _ => // no link, recur
            part.children.foreach(links)
        }
      case node: YNode if node.tagType == YType.Include => ramlInclude(node)
      case _                                            => part.children.foreach(links)
    }
  }

  private def ramlIncludeText(text: String): Unit = {
    val link = text.split("#").head
    if (link.nonEmpty) ramlInclude(link, LinkReference)
  }

  private def ramlInclude(node: YNode, reference: ReferenceKind = LinkReference): Unit = {
    node.value match {
      case scalar: YScalar => collector += (scalar.text, reference, node)
      case _               => eh.violation(InvalidInclude, "", s"Unexpected !include or dialect with ${node.value}", node)
    }
  }
}
