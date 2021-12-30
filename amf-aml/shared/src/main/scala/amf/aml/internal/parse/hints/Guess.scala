package amf.aml.internal.parse.hints

import amf.aml.client.scala.model.document.kind.AMLDocumentKind
import amf.core.internal.parser.Root

trait Guess[T <: AMLDocumentKind] {

  /**
   * Extracts the parsing hint from AST content
   * @param root parsed AST
   * @return
   */
  def hint(root: Root): Option[String]

  /**
    * Guesses the content of an AST. Returns Some[AMLDocumentKind] when it can match some known document kind, None otherwise.
    * @param root parsed AST
    * @return
    */
  def from(root: Root): Option[T]
}
