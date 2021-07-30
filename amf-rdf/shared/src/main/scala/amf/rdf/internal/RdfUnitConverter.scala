package amf.rdf.internal

import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.{AMFGraphConfiguration, config}
import amf.core.internal.unsafe.PlatformSecrets

object RdfUnitConverter extends RdfPlatformSecrets {

  def fromNativeRdfModel(id: String, rdfModel: RdfModel, conf: AMFGraphConfiguration): BaseUnit = {
    RdfModelParser(conf).parse(rdfModel, id)
  }

  def toNativeRdfModel(unit: BaseUnit, renderOptions: RenderOptions = config.RenderOptions()): RdfModel = {
    framework.unitToRdfModel(unit, renderOptions)
  }
}
