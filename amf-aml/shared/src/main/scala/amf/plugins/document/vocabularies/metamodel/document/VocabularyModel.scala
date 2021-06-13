package amf.plugins.document.vocabularies.metamodel.document

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Str}
import amf.core.internal.metamodel.document.{BaseUnitModel, ModuleModel}
import amf.core.internal.metamodel.domain.{ModelVocabularies, ModelDoc, ExternalModelVocabularies}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.metamodel.domain.{ExternalModel, VocabularyReferenceModel}
import amf.plugins.document.vocabularies.model.document.Vocabulary

object VocabularyModel extends ModuleModel with ExternalContextModel {

  val Name =
    Field(Str, Namespace.Core + "name", ModelDoc(ModelVocabularies.Core, "name", "Name for an entity"))
  val Base = Field(Str,
                   Namespace.Meta + "base",
                   ModelDoc(ModelVocabularies.Meta, "base", "Base URI prefix for definitions in this vocabulary"))
  val Imports = Field(Array(VocabularyReferenceModel),
                      Namespace.Owl + "imports",
                      ModelDoc(ExternalModelVocabularies.Owl, "import", "import relationships between vocabularies"))

  override def modelInstance: AmfObject = Vocabulary()

  override val `type`: List[ValueType] =
    Namespace.Meta + "Vocabulary" :: Namespace.Owl + "Ontology" :: BaseUnitModel.`type`

  override val fields: List[Field] =
    Name :: Imports :: Externals :: Declares :: Base :: BaseUnitModel.Location :: BaseUnitModel.fields

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Meta,
      "Vocabulary",
      "Basic primitives for the declaration of vocabularies."
  )
}
