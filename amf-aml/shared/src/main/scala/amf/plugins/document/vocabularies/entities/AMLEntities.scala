package amf.plugins.document.vocabularies.entities

import amf.core.entities.Entities
import amf.core.metamodel.Obj
import amf.plugins.document.vocabularies.metamodel.document._
import amf.plugins.document.vocabularies.metamodel.domain._

private[amf] object AMLEntities extends Entities {

  override protected val innerEntities: Seq[Obj] = Seq(
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
      DialectInstancePatchModel
  )

}
