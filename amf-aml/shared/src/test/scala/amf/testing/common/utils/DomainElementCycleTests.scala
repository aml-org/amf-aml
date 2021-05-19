package amf.testing.common.utils

import amf.client.environment.AMLConfiguration
import amf.core.errorhandling.UnhandledErrorHandler
import amf.core.io.FileAssertionTest
import amf.core.model.document.BaseUnit
import amf.core.model.domain.DomainElement
import amf.core.parser.SyamlParsedDocument
import amf.core.remote.Hint
import amf.plugins.document.vocabularies.emitters.instances.AmlDomainElementEmitter
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstanceUnit}
import amf.plugins.syntax.SYamlSyntaxPlugin
import org.scalatest.{Assertion, AsyncFunSuite}
import org.yaml.model.{YDocument, YNode}

import scala.concurrent.Future

trait DomainElementCycleTests
    extends AsyncFunSuite
    with FileAssertionTest
    with AMLParsingHelper
    with DefaultAMLInitialization
    with DialectRegistrationHelper {

  val basePath: String
  val baseHint: Hint

  protected def renderElement(dialect: String,
                              source: String,
                              extractor: BaseUnit => Option[DomainElement],
                              golden: String,
                              hint: Hint = baseHint,
                              directory: String = basePath): Future[Assertion] = {

    withDialect(s"file://$directory/$dialect") { (d, amlConfig) =>
      cycleElement(d, source, extractor, golden, hint, amlConfig, directory = directory)
    }
  }

  final def cycleElement(dialect: Dialect,
                         source: String,
                         extractor: BaseUnit => Option[DomainElement],
                         golden: String,
                         hint: Hint,
                         amlConfig: AMLConfiguration,
                         directory: String = basePath): Future[Assertion] = {
    for {
      b <- parse(s"file://$directory/$source", platform, hint, amlConfig)
      t <- Future.successful { transform(b) }
      s <- Future.successful { renderDomainElement(extractor(t), t.asInstanceOf[DialectInstanceUnit], dialect) } // generated string
      d <- writeTemporaryFile(s"$directory/$golden")(s)
      r <- assertDifferences(d, s"$directory/$golden")
    } yield r
  }

  def renderDomainElement(element: Option[DomainElement], instance: DialectInstanceUnit, dialect: Dialect): String = {
    val node     = element.map(AmlDomainElementEmitter.emit(_, dialect, UnhandledErrorHandler)).getOrElse(YNode.Empty)
    val document = SyamlParsedDocument(document = YDocument(node))
    SYamlSyntaxPlugin.unparse("application/yaml", document).getOrElse("").toString
  }
  def transform(unit: BaseUnit): BaseUnit = unit
}
