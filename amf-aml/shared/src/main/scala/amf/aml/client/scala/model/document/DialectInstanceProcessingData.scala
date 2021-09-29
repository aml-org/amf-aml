package amf.aml.client.scala.model.document
import amf.aml.internal.metamodel.document.DialectInstanceProcessingDataModel
import amf.aml.internal.metamodel.document.DialectInstanceProcessingDataModel._
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.document.BaseUnitProcessingData
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.internal.remote.{AmlDialectSpec, Spec}

case class DialectInstanceProcessingData(override val fields: Fields, override val annotations: Annotations)
    extends BaseUnitProcessingData(fields, annotations) {
  def graphDependencies: Seq[StrField] = fields.field(GraphDependencies)
  def definedBy(): StrField            = fields.field(DefinedBy)

  def withDefinedBy(dialectIri: String): DialectInstanceProcessingData = set(DefinedBy, dialectIri)
  def withGraphDependencies(dialectIris: Seq[String]): DialectInstanceProcessingData =
    set(GraphDependencies, dialectIris)

  override protected[amf] def sourceSpecProvider: Option[Spec] = sourceSpec.option().map(AmlDialectSpec)

  override def meta: DialectInstanceProcessingDataModel.type = DialectInstanceProcessingDataModel
}

object DialectInstanceProcessingData {
  def apply(): DialectInstanceProcessingData = apply(Annotations())
  def apply(annotations: Annotations): DialectInstanceProcessingData =
    DialectInstanceProcessingData(Fields(), annotations)
}
