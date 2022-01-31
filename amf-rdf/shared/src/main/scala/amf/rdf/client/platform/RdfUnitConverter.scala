package amf.rdf.client.platform

import amf.core.client.platform.config.RenderOptions
import amf.core.client.platform.model.document.BaseUnit
import amf.core.client.platform.{AMFGraphConfiguration, config}
import amf.rdf.internal.RdfModelParser
import amf.rdf.internal.unsafe.RdfPlatformSecrets

import amf.rdf.internal.convert.RdfClientConverter._
import amf.rdf.client.scala.{RdfUnitConverter => InternalRdfUnitConverter}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
@JSExportTopLevel("RdfUnitConverter")
object RdfUnitConverter extends RdfPlatformSecrets {

  def fromNativeRdfModel(id: String, rdfModel: RdfModel, conf: AMFGraphConfiguration): BaseUnit = {
    InternalRdfUnitConverter.fromNativeRdfModel(id, rdfModel, conf)
  }

  def toNativeRdfModel(unit: BaseUnit, renderOptions: RenderOptions = new RenderOptions()): RdfModel = {
    toNativeRdfModel(unit, AMFGraphConfiguration.predefined(), renderOptions)
  }

  def toNativeRdfModel(unit: BaseUnit, config: AMFGraphConfiguration, renderOptions: RenderOptions): RdfModel = {
    framework.unitToRdfModel(unit, config, renderOptions)
  }
}
