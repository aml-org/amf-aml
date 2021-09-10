package amf.testing.unclassified

import amf.aml.client.platform.{AMLConfiguration => ClientAMLConfiguration}
import amf.aml.client.scala.AMLConfiguration
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.domain.{DomainElement => ClientDomainElement}
import amf.core.client.scala.model.domain.DomainElement
import amf.testing.common.utils.DomainElementCycleTests
import org.yaml.builder.YamlOutputBuilder

trait ClientDomainElementTests extends DomainElementCycleTests {
  override def renderDomainElement(element: Option[DomainElement], config: AMLConfiguration): String = {
    val clientConfig: ClientAMLConfiguration = config
    element
      .map { element =>
        val stringBuilder                      = YamlOutputBuilder()
        val clientElement: ClientDomainElement = element
        clientConfig.elementClient().renderToBuilder(clientElement, stringBuilder)
        stringBuilder.result.toString
      }
      .getOrElse("")
  }
}
