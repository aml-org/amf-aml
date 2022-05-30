package amf.aml.client.scala.model.document

import amf.aml.internal.metamodel.document.DialectInstanceModel
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.traversal.{
  DomainElementSelectorAdapter,
  DomainElementTransformationAdapter,
  TransformationData,
  TransformationTraversal
}
import amf.core.internal.metamodel.document.DocumentModel.Encodes
import amf.core.internal.metamodel.document.ModuleModel.Declares
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.internal.remote.Spec
import amf.core.internal.unsafe.PlatformSecrets

case class DialectInstance(fields: Fields, annotations: Annotations)
    extends DialectInstanceUnit
    with DeclaresModel
    with EncodesModel
    with ComposedInstancesSupport
    with PlatformSecrets {

  override def meta: DialectInstanceModel.type = DialectInstanceModel

  def encodes: DomainElement       = fields.field(Encodes)
  def declares: Seq[DomainElement] = fields.field(Declares)

  private[amf] override def componentId: String = ""

  override def transform(
      selector: DomainElement => Boolean,
      transformation: (DomainElement, Boolean) => Option[DomainElement]
  )(implicit errorHandler: AMFErrorHandler): BaseUnit = {
    val domainElementAdapter  = new DomainElementSelectorAdapter(selector)
    val transformationAdapter = new DomainElementTransformationAdapter(transformation)
    val transformationData    = TransformationData(domainElementAdapter, transformationAdapter)
    val multiVisitAllowed     = if (isSelfEncoded) Set(id) else Set.empty[String]
    new TransformationTraversal(transformationData, multiVisitAllowed).traverse(this)
    this
  }

  private[amf] def isSelfEncoded: Boolean = encodes.id == id

  override def sourceSpec: Option[Spec] = processingData.sourceSpecProvider
}

object DialectInstance {
  def apply(): DialectInstance = apply(Annotations())

  def apply(annotations: Annotations): DialectInstance =
    DialectInstance(Fields(), annotations)
}
