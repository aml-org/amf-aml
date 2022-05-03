package amf.aml.internal.parse.dialects

import amf.core.client.scala.model.document.RecursiveUnit
import amf.core.client.scala.parse.document.{ParserContext, SyamlBasedParserErrorHandler}
import amf.core.internal.utils.QName
import amf.aml.internal.parse.common.{DeclarationContext, SyntaxErrorReporter}
import amf.core.client.scala.model.domain.extensions.CustomDomainProperty
import amf.core.internal.datanode.DataNodeParserContext
import amf.core.internal.parser.domain.{FragmentRef, SearchScope}
import amf.core.internal.plugins.syntax.SYamlAMFParserErrorHandler

class DialectContext(private val wrapped: ParserContext, private val ds: Option[DialectDeclarations] = None)
    extends SyamlBasedParserErrorHandler(
        wrapped.rootContextDocument,
        wrapped.refs,
        wrapped.futureDeclarations,
        wrapped.config
    )
    with DialectSyntax
    with DeclarationContext
    with SyntaxErrorReporter
    with DataNodeParserContext {

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

  override def findAnnotation(key: String, scope: SearchScope.Scope): Option[CustomDomainProperty] = None
  override def getMaxYamlReferences: _root_.scala.Option[Int] = wrapped.config.parsingOptions.maxYamlReferences
  override def fragments: Map[String, FragmentRef]            = Map.empty
}
