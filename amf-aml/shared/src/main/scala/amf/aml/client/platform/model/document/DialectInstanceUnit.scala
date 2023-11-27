package amf.aml.client.platform.model.document

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.aml.internal.convert.VocabulariesClientConverter.ClientList
import amf.core.client.platform.model.StrField
import amf.core.client.platform.model.document.BaseUnit
import com.github.ghik.silencer.silent

import scala.scalajs.js.annotation.JSExportAll

@JSExportAll
trait DialectInstanceUnit extends BaseUnit {
  override private[amf] val _internal: amf.aml.client.scala.model.document.DialectInstanceUnit

  override def processingData: DialectInstanceProcessingData = _internal.processingData

  def withProcessingData(data: DialectInstanceProcessingData): this.type = {
    _internal.withProcessingData(data._internal)
    this
  }

//  @deprecated("Use processingData.definedBy instead", "AML 6.0.0")
  @silent
  def definedBy(): StrField = _internal.definedBy()

//  @deprecated("Use processingData.graphDependencies instead", "AML 6.0.0")
  @silent
  def graphDependencies(): ClientList[StrField] = _internal.graphDependencies.asClient

//  @deprecated("Use processingData.withDefinedBy instead", "AML 6.0.0")
  @silent
  def withDefinedBy(dialectId: String): this.type = {
    _internal.withDefinedBy(dialectId)
    this
  }

//  @deprecated("Use processingData.withGraphDependencies instead", "AML 6.0.0")
  @silent
  def withGraphDependencies(ids: ClientList[String]): this.type = {
    _internal.withGraphDependencies(ids.asInternal)
    this
  }

}
