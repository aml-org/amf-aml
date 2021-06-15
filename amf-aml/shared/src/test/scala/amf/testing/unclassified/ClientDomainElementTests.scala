package amf.testing.unclassified

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.aml.client.platform.model.document.{Dialect => ClientDialect}
import amf.aml.client.scala.AMLConfiguration
import amf.core.client.platform.model.domain.{DomainElement => ClientDomainElement}
import amf.core.client.scala.errorhandling.DefaultErrorHandler
import amf.aml.client.platform.render.{AmlDomainElementEmitter => ClientAmlDomainElementEmitter}
import amf.core.internal.convert.ClientErrorHandlerConverter.ErrorHandlerConverter
import amf.core.client.scala.model.domain.DomainElement
import amf.aml.client.scala.model.document.{Dialect, DialectInstanceUnit}
import amf.testing.common.utils.DomainElementCycleTests
import org.yaml.builder.YamlOutputBuilder

trait ClientDomainElementTests extends DomainElementCycleTests {
  override def renderDomainElement(element: Option[DomainElement],
                                   instance: DialectInstanceUnit,
                                   dialect: Dialect,
                                   config: AMLConfiguration): String = {
    val eh = ErrorHandlerConverter.asClient(DefaultErrorHandler())
    element
      .map { element =>
        val stringBuilder                      = YamlOutputBuilder()
        val clientDialect: ClientDialect       = dialect
        val clientElement: ClientDomainElement = element
        ClientAmlDomainElementEmitter.emitToBuilder(clientElement, clientDialect, eh, stringBuilder)
        stringBuilder.result.toString
      }
      .getOrElse("")
  }
}
