package amf.aml.client.platform.render
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.aml.client.platform.model.document.Dialect
import amf.core.client.platform.errorhandling.ClientErrorHandler
import amf.core.client.platform.model.domain.DomainElement
import amf.core.internal.convert.ClientErrorHandlerConverter.ErrorHandlerConverter
import amf.core.internal.render.YNodeDocBuilderPopulator
import org.yaml.builder.DocBuilder
import amf.aml.client.scala.render.{
  AmlDomainElementEmitter => InternalAmlDomainElementEmitter
}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("AmlDomainElementEmitter")
object AmlDomainElementEmitter {

  def emitToBuilder[T](element: DomainElement,
                       emissionStructure: Dialect,
                       eh: ClientErrorHandler,
                       builder: DocBuilder[T]): Unit = {
    val node = InternalAmlDomainElementEmitter.emit(asInternal(element),
                                                    asInternal(emissionStructure),
                                                    ErrorHandlerConverter.asInternal(eh))
    YNodeDocBuilderPopulator.populate(node, builder)
  }
}
