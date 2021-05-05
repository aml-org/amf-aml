package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Iri, SortedArray}
import amf.core.metamodel.domain.{DomainElementModel, ExternalModelVocabularies, ModelDoc}
import amf.core.vocabulary.Namespace

trait HasObjectRangeModel extends DomainElementModel {
  val ObjectRange: Field = Field(
      SortedArray(Iri),
      Namespace.Shacl + "node",
      ModelDoc(ExternalModelVocabularies.Shacl,
               "range",
               "Object constraint over the type of the mapped graph property")
  )
}
