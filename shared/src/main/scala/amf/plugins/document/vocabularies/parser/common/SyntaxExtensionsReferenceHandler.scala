package amf.plugins.document.vocabularies.parser.common

import amf.core.parser.{LibraryReference, LinkReference, ReferenceHandler, _}
import amf.plugins.document.vocabularies.DialectsRegistry
import amf.plugins.features.validation.CoreValidations.InvalidInclude
import amf.validation.DialectValidations.InvalidModuleType
import org.yaml.model._

class SyntaxExtensionsReferenceHandler(registry: DialectsRegistry, eh: ErrorHandler) extends ReferenceHandler {
  private val collector = ReferenceCollector()

  override def collect(parsedDoc: ParsedDocument, ctx: ParserContext): ReferenceCollector = {
    parsedDoc match {
      case parsed: SyamlParsedDocument =>

        for (comment <- parsed.comment)
          if (referencesDialect(comment))
            collector += (dialectDefinitionUrl(comment), SchemaReference, parsed.document.node)

        libraries(parsed.document, ctx)
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

  private def libraries(document: YDocument, ctx: ParserContext): Unit = {
    document.to[YMap] match {
      case Right(map) =>
        map
          .key("uses")
          .foreach { entry =>
            entry.value.to[YMap] match {
              case Right(m) => m.entries.foreach(library)
              case _        => ctx.violation(InvalidModuleType, "", s"Expected map but found: ${entry.value}", entry.value)
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
    collector += (entry.value, LibraryReference, entry.value)
  }

  private def links(part: YPart): Unit = {
    part match {
      case entry: YMapEntry =>
        entry.key.as[YScalar].text match {
          case "$target" => // patch $target link
            val includeRef = entry.value
            ramlInclude(includeRef)

          case "$dialect" => // $dialect link
            val dialectRef = entry.value
            if (!registry.knowsHeader(s"%${dialectRef.as[String]}")) {
              ramlIncludeText(dialectRef)
            }

          case "$include" => // !include as $include link
            val includeRef = entry.value
            ramlInclude(includeRef)

          case "$ref" => // $ref link
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
    if (link.nonEmpty) ramlInclude(link)
  }

  private def ramlInclude(node: YNode): Unit = {
    node.value match {
      case scalar: YScalar => collector += (scalar.text, LinkReference, node)
      case _               => eh.violation(InvalidInclude, "", s"Unexpected !include or dialect with ${node.value}", node)
    }
  }
}
