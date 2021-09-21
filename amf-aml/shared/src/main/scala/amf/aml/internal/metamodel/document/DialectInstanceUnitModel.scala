package amf.aml.internal.metamodel.document

import amf.core.client.scala.vocabulary.Namespace
import amf.core.client.scala.vocabulary.Namespace.Document
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri}
import amf.core.internal.metamodel.document.{BaseUnitModel, BaseUnitProcessingDataModel}
import amf.core.internal.metamodel.domain.{ModelDoc, ModelVocabularies}

trait DialectInstanceUnitModel { self: BaseUnitModel =>
  @deprecated("Use DialectInstanceProcessingDataModel.DefinedBy instead", "AML 6.0.0")
  val DefinedBy: Field = Field(Iri, Namespace.Meta + "definedBy", deprecated = true)

  @deprecated("Use DialectInstanceProcessingDataModel.GraphDependencies instead", "AML 6.0.0")
  val GraphDependencies: Field = Field(Array(Iri), Namespace.Document + "graphDependencies", deprecated = true)

  override val ProcessingData: Field =
    Field(
        DialectInstanceProcessingDataModel,
        Document + "processingData",
        ModelDoc(ModelVocabularies.AmlDoc,
                 "processingData",
                 "Field with utility data to be used in Base Unit processing")
    )
}
