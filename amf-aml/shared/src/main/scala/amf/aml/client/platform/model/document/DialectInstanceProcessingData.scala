package amf.aml.client.platform.model.document
import amf.aml.client.scala.model.document.{DialectInstanceProcessingData => InternalDialectInstanceProcessingData}
import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.StrField
import amf.core.client.platform.model.document.BaseUnitProcessingData

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class DialectInstanceProcessingData(override private[amf] val _internal: InternalDialectInstanceProcessingData)
    extends BaseUnitProcessingData {

  @JSExportTopLevel("DialectInstanceProcessingData")
  def this() = this(InternalDialectInstanceProcessingData())

  def definedBy(): StrField = _internal.definedBy()

  def graphDependencies(): ClientList[StrField] = _internal.graphDependencies.asClient

  def withDefinedBy(dialectId: String): DialectInstanceProcessingData = {
    _internal.withDefinedBy(dialectId)
    this
  }

  def withGraphDependencies(ids: ClientList[String]): DialectInstanceProcessingData = {
    _internal.withGraphDependencies(ids.asInternal)
    this
  }

}
