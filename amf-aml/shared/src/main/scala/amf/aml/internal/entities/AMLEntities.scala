package amf.aml.internal.entities

import amf.core.internal.entities.Entities
import amf.core.internal.metamodel.ModelDefaultBuilder
import amf.aml.internal.metamodel.document._
import amf.aml.internal.metamodel.domain._
import amf.core.internal.metamodel.document.{
  BaseUnitProcessingDataModel,
  BaseUnitSourceInformationModel,
  LocationInformationModel
}

private[amf] object AMLEntities extends Entities {

  override protected val innerEntities: Seq[ModelDefaultBuilder] = Seq(
      VocabularyModel,
      ExternalModel,
      VocabularyReferenceModel,
      ClassTermModel,
      ObjectPropertyTermModel,
      DatatypePropertyTermModel,
      DialectModel,
      NodeMappingModel,
      UnionNodeMappingModel,
      PropertyMappingModel,
      DocumentsModelModel,
      PublicNodeMappingModel,
      DocumentMappingModel,
      DialectLibraryModel,
      DialectFragmentModel,
      DialectInstanceModel,
      DialectInstanceLibraryModel,
      DialectInstanceFragmentModel,
      DialectInstancePatchModel,
      DialectInstanceProcessingDataModel,
      SemanticExtensionModel,
      AnnotationMappingModel,
      BaseUnitProcessingDataModel,
      BaseUnitSourceInformationModel,
      LocationInformationModel,
      AnyMappingModel
  )

}
