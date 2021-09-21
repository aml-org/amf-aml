package amf.aml.client.scala.model.document

import amf.aml.internal.metamodel.document.DialectInstanceModel.{DefinedBy, GraphDependencies, ProcessingData}
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.document.BaseUnit
import amf.core.internal.metamodel.document.BaseUnitModel.References

trait DialectInstanceUnit extends BaseUnit with ExternalContext[DialectInstanceUnit] {

  @deprecated("Use processingData.graphDependencies instead", "AML 6.0.0")
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)

  @deprecated("Use processingData.definedBy instead", "AML 6.0.0")
  def definedBy(): StrField = fields.field(DefinedBy)

  def references: Seq[BaseUnit] = fields.field(References)

  @deprecated("Use processingData.withDefinedBy instead", "AML 6.0.0")
  def withDefinedBy(dialectIri: String): this.type = set(DefinedBy, dialectIri)

  @deprecated("Use processingData.withGraphDependencies instead", "AML 6.0.0")
  def withGraphDependencies(dialectIris: Seq[String]): this.type = set(GraphDependencies, dialectIris)

  override def processingData: DialectInstanceProcessingData = fields.field(ProcessingData)

  override private[amf] def setDefaultProcessingData(): Unit = {
    withProcessingData {
      DialectInstanceProcessingData().withTransformed(false)
    }
  }
}
