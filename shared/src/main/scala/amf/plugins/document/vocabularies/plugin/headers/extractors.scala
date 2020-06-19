package amf.plugins.document.vocabularies.plugin.headers

import amf.core.Root
import amf.core.parser.SyamlParsedDocument
import amf.plugins.document.vocabularies.model.document.Dialect
import org.yaml.model.{YComment, YDocument, YMap}
import amf.core.parser._
import amf.plugins.document.vocabularies.AMLPlugin

trait RamlHeaderExtractor {
  def comment(root: Root): Option[String]            = root.parsed.comment
  def comment(document: YDocument): Option[YComment] = document.children.collectFirst({ case c: YComment => c })
}

trait JsonHeaderExtractor {
  def dialect(root: Root): Option[String] = {
    root.parsed match {
      case parsedInput: SyamlParsedDocument => dialectForDoc(parsedInput.document)
      case _                                => None
    }
  }

  private def dialectForDoc(document: YDocument): Option[String] = {
    document.node
      .toOption[YMap]
      .map(_.entries)
      .getOrElse(Nil)
      .collectFirst({ case e if e.key.asScalar.exists(_.text == "$dialect") => e })
      .flatMap(e => e.value.asScalar.map(_.text))
  }
}

trait KeyPropertyHeaderExtractor {
  def dialectByKeyProperty(root: YDocument): Option[Dialect] =
    AMLPlugin.registry
      .allDialects()
      .find(d => Option(d.documents()).exists(_.keyProperty().value()) && containsVersion(root, d))

  def dialectInKey(root: Root): Option[Dialect] =
    root.parsed match {
      case parsedInput: SyamlParsedDocument => dialectByKeyProperty(parsedInput.document)
      case _                                => None
    }

  private def containsVersion(document: YDocument, d: Dialect): Boolean =
    document.node
      .toOption[YMap]
      .map(_.entries)
      .getOrElse(Nil)
      .collectFirst({ case e if e.key.asScalar.exists(scalar => d.name().is(scalar.text)) => e })
      .exists(e => { e.value.asScalar.exists(_.text == d.version().value()) })
}
