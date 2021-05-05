package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Iri, SortedArray, Str}
import amf.core.metamodel.domain.{DomainElementModel, ExternalModelVocabularies, ModelDoc, ModelVocabularies}
import amf.core.vocabulary.Namespace

/**
  * Mappings form with which graph properties can be derived (annotation mappings, property mappings)
  */
trait PropertyLikeMappingModel extends DomainElementModel with HasObjectRangeModel {
  val Name: Field = Field(Str,
                          Namespace.Core + "name",
                          ModelDoc(ModelVocabularies.Core, "name", "Name in the source AST for the mapping"))

  val LiteralRange: Field = Field(Iri,
                                  Namespace.Shacl + "datatype",
                                  ModelDoc(ExternalModelVocabularies.Shacl,
                                           "datatype",
                                           "Scalar constraint over the type of the mapped graph property"))

  val NodePropertyMapping: Field = Field(
      Iri,
      Namespace.Shacl + "path",
      ModelDoc(ExternalModelVocabularies.Shacl, "path", "URI for the mapped graph property derived from this mapping"))

}
