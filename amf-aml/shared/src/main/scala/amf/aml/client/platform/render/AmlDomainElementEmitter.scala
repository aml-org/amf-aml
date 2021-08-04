package amf.aml.client.platform.render
import amf.aml.client.platform.AMLConfiguration
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.aml.client.platform.model.document.Dialect
import amf.aml.client.scala.model.domain.NodeMapping
import amf.core.client.platform.errorhandling.ClientErrorHandler
import amf.core.client.platform.model.domain.DomainElement
import amf.core.internal.convert.ClientErrorHandlerConverter.ErrorHandlerConverter
import amf.core.internal.render.YNodeDocBuilderPopulator
import org.yaml.builder.DocBuilder
import amf.aml.client.scala.render.{AmlDomainElementEmitter => InternalAmlDomainElementEmitter}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("AmlDomainElementEmitter")
object AmlDomainElementEmitter {

  def emitToBuilder[T](element: DomainElement, amlConfig: AMLConfiguration, builder: DocBuilder[T]): Unit = {
    val internalElement = asInternal(element)
    amlConfig._internal
      .configurationState()
      .getDialects()
      .find(
          _.declares
            .collectFirst({ case nm: NodeMapping if internalElement.meta.`type`.exists(_.iri() == nm.id) => nm })
            .isDefined)
      .foreach { emissionStructure =>
        val handler = amlConfig._internal.errorHandlerProvider.errorHandler()
        val node    = InternalAmlDomainElementEmitter.emit(internalElement, emissionStructure, handler)
        YNodeDocBuilderPopulator.populate(node, builder)
      }
  }
}
