package amf.aml.internal.metamodel.domain

import amf.core.internal.metamodel.Type.{Bool, Str}
import amf.core.internal.metamodel.domain.{DomainElementModel, LinkableElementModel}
import amf.core.internal.metamodel.Field
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping}

class DialectDomainElementModel(
    val typeIri: Seq[String] = Seq(),
    val typeFields: Seq[Field] = Nil,
    val nodeMapping: Option[NodeMapping] = None
) extends DomainElementModel
    with LinkableElementModel {

  override val fields: List[Field] =
    DialectDomainElementModel.Abstract :: DialectDomainElementModel.DeclarationName :: DomainElementModel.fields ++ LinkableElementModel.fields ++ typeFields
  override val `type`: List[ValueType] = typeIri
    .map(iriToValue)
    .toList ++ ((Namespace.Meta + "DialectDomainElement") :: DomainElementModel.`type`)
  def iriToValue(iri: String): ValueType = ValueType(iri)

  override def modelInstance: AmfObject = {
    val element = DialectDomainElement()
    nodeMapping.foreach(element.withDefinedBy)
    element.withInstanceTypes(typeIri ++ nodeMapping.map(_.id))
    element
  }

//    throw new Exception("DialectDomainElement is an abstract class and it cannot be instantiated directly")
}

object DialectDomainElementModel {
  def apply(): DialectDomainElementModel = new DialectDomainElementModel()
  def apply(typeIri: String)             = new DialectDomainElementModel(Seq(typeIri))

  val DeclarationName: Field = Field(Str, Namespace.Meta + "declarationName")
  val Abstract: Field        = Field(Bool, Namespace.Meta + "abstract")
}
