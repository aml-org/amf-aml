package amf.plugins.document.vocabularies.metamodel.domain
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.Str
import amf.core.internal.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.client.scala.vocabulary.Namespace

trait NodeMappableModel extends DomainElementModel {
  val Name: Field =
    Field(Str, Namespace.Core + "name", ModelDoc(ModelVocabularies.Core, "name", "Name of the node mappable element"))
}
