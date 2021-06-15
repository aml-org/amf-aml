package amf.aml.internal.metamodel.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Iri, SortedArray}
import amf.core.internal.metamodel.domain.{DomainElementModel, ExternalModelVocabularies, ModelDoc}
import amf.core.client.scala.vocabulary.Namespace

trait HasObjectRangeModel extends DomainElementModel {
  val ObjectRange: Field = Field(
      SortedArray(Iri),
      Namespace.Shacl + "node",
      ModelDoc(ExternalModelVocabularies.Shacl,
               "range",
               "Object constraint over the type of the mapped graph property")
  )
}
