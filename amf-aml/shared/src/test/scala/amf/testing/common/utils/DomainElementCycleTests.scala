package amf.testing.common.utils

import amf.aml.client.scala.AMLConfiguration
import amf.aml.client.scala.model.document.DialectInstanceUnit
import amf.aml.internal.render.plugin.AMLDialectInstanceRenderingPlugin
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.internal.plugins.syntax.SyamlSyntaxRenderPlugin
import amf.core.internal.remote.Hint
import amf.core.internal.remote.Mimes._
import amf.core.io.FileAssertionTest
import org.scalatest.{Assertion, AsyncFunSuite}
import org.yaml.model.{YDocument, YNode}

import java.io.StringWriter
import scala.concurrent.Future

trait DomainElementCycleTests
    extends AsyncFunSuite
    with FileAssertionTest
    with AMLParsingHelper
    with DialectRegistrationHelper {

  val basePath: String
  val baseHint: Hint

  protected def renderElement(dialect: String,
                              source: String,
                              extractor: BaseUnit => Option[DomainElement],
                              golden: String,
                              hint: Hint = baseHint,
                              directory: String = basePath,
                              baseConfig: AMLConfiguration = AMLConfiguration.predefined()): Future[Assertion] = {

    withDialect(s"file://$directory/$dialect") { (d, amlConfig) =>
      val nextConfig = baseConfig.configurationState().getDialects().foldLeft(amlConfig) { (config, dialect) =>
        config.withDialect(dialect)
      }
      cycleElement(source, extractor, golden, nextConfig, directory = directory)
    }
  }

  final def cycleElement(source: String,
                         extractor: BaseUnit => Option[DomainElement],
                         golden: String,
                         amlConfig: AMLConfiguration,
                         directory: String = basePath): Future[Assertion] = {
    for {
      b <- parse(s"file://$directory/$source", platform, amlConfig)
      t <- Future.successful { transform(b) }
      s <- Future.successful {
        renderDomainElement(extractor(t), amlConfig)
      } // generated string
      d <- writeTemporaryFile(s"$directory/$golden")(s)
      r <- assertDifferences(d, s"$directory/$golden")
    } yield r
  }

  def renderDomainElement(element: Option[DomainElement], config: AMLConfiguration): String = {
    val references = config.configurationState().getDialects()
    val node =
      element.map(config.elementClient().renderElement(_, references)).getOrElse(YNode.Empty)
    val document = SyamlParsedDocument(document = YDocument(node))
    val w        = new StringWriter
    SyamlSyntaxRenderPlugin.emit(`application/yaml`, document, w).map(_.toString).getOrElse("")
  }
  def transform(unit: BaseUnit): BaseUnit = unit
}
