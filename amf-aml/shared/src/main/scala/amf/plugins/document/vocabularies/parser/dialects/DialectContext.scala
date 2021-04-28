package amf.plugins.document.vocabularies.parser.dialects

import amf.core.model.document.RecursiveUnit
import amf.core.parser.ParserContext
import amf.core.utils.QName
import amf.plugins.document.vocabularies.parser.common.{DeclarationContext, SyntaxErrorReporter}

class DialectContext(private val wrapped: ParserContext, private val ds: Option[DialectDeclarations] = None)
    extends ParserContext(wrapped.rootContextDocument, wrapped.refs, wrapped.futureDeclarations, wrapped.eh)
    with DialectSyntax
    with DeclarationContext
    with SyntaxErrorReporter {

  def findInRecursiveUnits(key: String): Option[String] = {
    val qname = QName(key)
    if (qname.isQualified) {
      recursiveDeclarations.get(qname.qualification) match {
        case Some(recursiveUnit) =>
          val unitId = recursiveUnit.id.stripSuffix(recursiveUnit.componentId)
          Some(unitId + "#/declarations/" + qname.name)
        case _ => None
      }
    } else {
      None
    }
  }

  var recursiveDeclarations: Map[String, RecursiveUnit] = Map()

  override val declarations: DialectDeclarations =
    ds.getOrElse(new DialectDeclarations(errorHandler = eh, futureDeclarations = futureDeclarations))

}
