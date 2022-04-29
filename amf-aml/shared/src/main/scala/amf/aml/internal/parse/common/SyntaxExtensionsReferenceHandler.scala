package amf.aml.internal.parse.common

import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.aml.internal.validate.DialectValidations.InvalidModuleType
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.parse.document._
import amf.core.internal.parser.YMapOps
import amf.core.internal.plugins.syntax.SYamlAMFParserErrorHandler
import amf.core.internal.validation.CoreValidations.InvalidInclude
import org.yaml.model._

class SyntaxExtensionsReferenceHandler(errorHandler: AMFErrorHandler) extends ReferenceHandler {
  private val collector = CompilerReferenceCollector()

  implicit val eh: SYamlAMFParserErrorHandler = new SYamlAMFParserErrorHandler(errorHandler)

  override def collect(parsedDoc: ParsedDocument, ctx: ParserContext): CompilerReferenceCollector = {
    val dialects = ctx.config.sortedRootParsePlugins.collect { case plugin: AMLDialectInstanceParsingPlugin =>
      plugin.dialect.nameAndVersion()
    }
    collect(parsedDoc, dialects)
  }

  private def collect(parsedDoc: ParsedDocument, knownDialects: Seq[String]) = {
    parsedDoc match {
      case parsed: SyamlParsedDocument =>
        parsed.comment.filter(referencesDialect).foreach { comment =>
          collector += (dialectDefinitionUrl(comment), SchemaReference, parsed.document.node.location)
        }
        libraries(parsed.document)
        links(parsed.document, knownDialects)

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

  private def libraries(document: YDocument): Unit = {
    document.to[YMap] match {
      case Right(map) =>
        map
          .key("uses")
          .foreach { entry =>
            entry.value.to[YMap] match {
              case Right(m) => m.entries.foreach(library)
              case _ =>
                errorHandler
                  .violation(InvalidModuleType, "", s"Expected map but found: ${entry.value}", entry.value.location)
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

  private def library(entry: YMapEntry): Unit = {
    collector += (entry.value, LibraryReference, entry.value.location)
  }

  private def links(part: YPart, knownDialects: Seq[String]): Unit = {
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

          case "$dialect" => // $dialect link
            // If it is not a known dialect we assume that it is a URI
            entry.value.asScalar.map(_.text).foreach { dialectRef =>
              if (!knownDialects.contains(dialectRef)) ramlIncludeText(dialectRef)
            }

          case _ => // no link, recur
            part.children.foreach(child => links(child, knownDialects))
        }
      case node: YNode if node.tagType == YType.Include => ramlInclude(node)
      case _                                            => part.children.foreach(child => links(child, knownDialects))
    }
  }

  private def ramlIncludeText(text: String): Unit = {
    val link = text.split("#").head
    if (link.nonEmpty) ramlInclude(link, LinkReference)
  }

  private def ramlInclude(node: YNode, reference: ReferenceKind = LinkReference): Unit = {
    node.value match {
      case scalar: YScalar => collector += (scalar.text, reference, node.location)
      case _ =>
        errorHandler.violation(InvalidInclude, "", s"Unexpected !include or dialect with ${node.value}", node.location)
    }
  }
}
