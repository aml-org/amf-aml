package amf.aml.internal.metamodel.document

import amf.aml.client.scala.model.document.DialectInstanceProcessingData
import amf.core.client.scala.vocabulary.Namespace.{ApiContract, Document}
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri, Str}
import amf.core.internal.metamodel.document.BaseUnitProcessingDataModel
import amf.core.internal.metamodel.domain.{ModelDoc, ModelVocabularies}

object DialectInstanceProcessingDataModel extends BaseUnitProcessingDataModel {
  val DefinedBy: Field = Field(
      Iri,
      Namespace.Meta + "definedBy",
      ModelDoc(ModelVocabularies.Meta, "definedBy", "Dialect used to parse this Dialect Instance"))

  val GraphDependencies: Field = Field(
      Array(Iri),
      Namespace.Document + "graphDependencies",
      ModelDoc(ModelVocabularies.Meta,
               "graphDependencies",
               "Other dialects referenced to parse specific nodes in this Dialect Instance")
  )

  override def modelInstance: DialectInstanceProcessingData = DialectInstanceProcessingData()

  override def fields: List[Field] = List(DefinedBy, GraphDependencies) ++ BaseUnitProcessingDataModel.fields

  override val `type`: List[ValueType] = List(Document + "DialectInstanceProcessingData")

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.AmlDoc,
      "APIContractProcessingData",
      "Class that groups data related to how a Base Unit was processed",
      Seq((Document + "BaseUnitProcessingData").iri())
  )
}
