package amf.plugins.document.vocabularies.metamodel.domain
import amf.core.metamodel.Field
import amf.core.metamodel.Type.Str
import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.vocabulary.Namespace

trait NodeMappableModel {
  val Name = Field(Str, Namespace.Core + "name",
    ModelDoc(ModelVocabularies.Core, "name", "Name of the node mappable element"))
}
