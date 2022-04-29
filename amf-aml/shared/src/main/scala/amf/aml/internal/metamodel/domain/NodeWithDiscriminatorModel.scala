package amf.aml.internal.metamodel.domain
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Str
import amf.core.internal.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.client.scala.vocabulary.Namespace

trait NodeWithDiscriminatorModel extends DomainElementModel with HasObjectRangeModel {
  val TypeDiscriminator: Field = Field(
      Str,
      Namespace.Meta + "typeDiscriminatorMap",
      ModelDoc(
          ModelVocabularies.Meta,
          "typeDiscriminatorMap",
          "Information about the discriminator values in the source AST for the property mapping"
      )
  )
  val TypeDiscriminatorName: Field = Field(
      Str,
      Namespace.Meta + "typeDiscriminatorName",
      ModelDoc(
          ModelVocabularies.Meta,
          "typeDiscriminatorName",
          "Information about the field in the source AST to be used as discrimintaro in the property mapping"
      )
  )
}
