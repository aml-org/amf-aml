package amf.aml.internal.metamodel.domain

import amf.aml.client.scala.model.domain.ConditionalNodeMapping
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Iri
import amf.core.internal.metamodel.domain._

object ConditionalNodeMappingModel extends AnyMappingModel {

  val If: Field = Field(
      Iri,
      Namespace.AmfAml + "if",
      ModelDoc(ExternalModelVocabularies.Shacl,
               "if",
               "Conditional constraint if over the type of the mapped graph property")
  )

  val Then: Field = Field(
      Iri,
      Namespace.AmfAml + "then",
      ModelDoc(ExternalModelVocabularies.Shacl,
               "then",
               "Conditional constraint then over the type of the mapped graph property")
  )

  val Else: Field = Field(
      Iri,
      Namespace.AmfAml + "else",
      ModelDoc(ExternalModelVocabularies.Shacl,
               "else",
               "Conditional constraint else over the type of the mapped graph property")
  )

  override val fields
    : List[Field] = Name :: If :: Then :: Else :: LinkableElementModel.fields ++ DomainElementModel.fields ++ AnyMappingModel.fields

  override val `type`
    : List[ValueType] = Namespace.Meta + "ConditionalNodeMapping" :: Namespace.Shacl + "Shape" :: DomainElementModel.`type`

  override def modelInstance: AmfObject = ConditionalNodeMapping()
}
