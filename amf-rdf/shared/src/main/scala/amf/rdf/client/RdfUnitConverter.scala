package amf.rdf.client

import amf.core.client.platform.config.RenderOptions
import amf.core.client.platform.model.document.BaseUnit
import amf.rdf.internal.RdfModel
import amf.rdf.internal.{RdfUnitConverter => InternalConverter}
import amf.core.internal.convert.CoreClientConverters._

object RdfUnitConverter {

  def toNativeRdfModel(unit: BaseUnit, renderOptions: RenderOptions = new RenderOptions()): RdfModel = {
    val coreOptions = renderOptions
    InternalConverter.toNativeRdfModel(unit, coreOptions)
  }
}
