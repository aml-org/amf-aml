package amf.aml.internal.metamodel.domain

import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri}
import amf.core.internal.metamodel.domain._

import scala.::

trait AnyMappingModel
    extends DomainElementModel
    with LinkableElementModel
    with MergeableMappingModel
    with NodeMappableModel {

  val And: Field = Field(
      Array(Iri),
      Namespace.AmfAml + "and",
      ModelDoc(ExternalModelVocabularies.Shacl, "and", "Logical and composition of data")
  )

  val Or: Field = Field(
      Array(Iri),
      Namespace.AmfAml + "or",
      ModelDoc(ExternalModelVocabularies.Shacl, "or", "Logical or composition of data")
  )

  val Components: Field = Field(
      Array(Iri),
      Namespace.AmfAml + "components",
      ModelDoc(ExternalModelVocabularies.Shacl,
               "components",
               "Array of component mappings in case of component combination generated mapping")
  )

  override val `type`
    : List[ValueType] = Namespace.Meta + "AndMapping" :: Namespace.Shacl + "Shape" :: DomainElementModel.`type`

}

object AnyMappingModel extends AnyMappingModel {

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Shapes,
      "AnyMapping",
      "Base class for all mappings stored in the AML graph model",
      superClasses = Seq((Namespace.Shacl + "Shape").iri())
  )

  override def modelInstance = throw new Exception("AnyMapping is an abstract class")

  override val fields: List[Field] = List(And, Or, Components)
}
