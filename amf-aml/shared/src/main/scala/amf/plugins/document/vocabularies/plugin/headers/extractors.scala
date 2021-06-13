package amf.plugins.document.vocabularies.plugin.headers

import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.internal.parser.{Root, YNodeLikeOps}
import org.yaml.model.{YComment, YDocument, YMap}

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

trait KeyPropertyHeaderExtractor {}
