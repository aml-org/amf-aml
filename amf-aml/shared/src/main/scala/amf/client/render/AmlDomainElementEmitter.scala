package amf.client.render
import amf.client.convert.VocabulariesClientConverter._
import amf.client.model.document.Dialect
import amf.client.model.domain.DomainElement
import amf.client.resolve.ClientErrorHandler
import amf.client.resolve.ClientErrorHandlerConverter.ErrorHandlerConverter
import amf.core.emitter.YNodeDocBuilderPopulator
import org.yaml.builder.DocBuilder
import amf.plugins.document.vocabularies.emitters.instances.{AmlDomainElementEmitter => InternalAmlDomainElementEmitter}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("AmlDomainElementEmitter")
object AmlDomainElementEmitter {

  def emitToBuilder[T](element: DomainElement,
                       emissionStructure: Dialect,
                       eh: ClientErrorHandler,
                       builder: DocBuilder[T]): Unit = {
    val node = InternalAmlDomainElementEmitter.emit(asInternal(element), asInternal(emissionStructure), ErrorHandlerConverter.asInternal(eh))
    YNodeDocBuilderPopulator.populate(node, builder)
  }
}
