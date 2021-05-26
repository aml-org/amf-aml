package amf.plugins.document.vocabularies.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Any, Bool, Double, Int, Iri, SortedArray, Str}
import amf.core.metamodel.domain.{DomainElementModel, ExternalModelVocabularies, ModelDoc, ModelVocabularies}
import amf.core.vocabulary.Namespace

/**
  * Mappings form with which graph properties can be derived (annotation mappings, property mappings)
  */
trait PropertyLikeMappingModel extends DomainElementModel with HasObjectRangeModel with NodeWithDiscriminatorModel {
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

  val Sorted: Field = Field(Bool,
                            Namespace.Meta + "sorted",
                            ModelDoc(ModelVocabularies.Meta,
                                     "sorted",
                                     "Marks the mapping as requiring order in the mapped collection of nodes"))
  val MinCount: Field = Field(
      Int,
      Namespace.Shacl + "minCount",
      ModelDoc(ExternalModelVocabularies.Shacl, "minCount", "Minimum count constraint over the mapped property"))

  val Pattern: Field = Field(
      Str,
      Namespace.Shacl + "pattern",
      ModelDoc(ExternalModelVocabularies.Shacl, "pattern", "Pattern constraint over the mapped property"))

  val Minimum: Field = Field(
      Double,
      Namespace.Shacl + "minInclusive",
      ModelDoc(ExternalModelVocabularies.Shacl,
               "minInclusive",
               "Minimum inclusive constraint over the mapped property")
  )

  val Maximum: Field = Field(
      Double,
      Namespace.Shacl + "maxInclusive",
      ModelDoc(ExternalModelVocabularies.Shacl,
               "maxInclusive",
               "Maximum inclusive constraint over the mapped property")
  )

  val AllowMultiple: Field = Field(
      Bool,
      Namespace.Meta + "allowMultiple",
      ModelDoc(ModelVocabularies.Meta, "allowMultiple", "Allows multiple mapped nodes for the property mapping"))

  val Enum: Field = Field(
      SortedArray(Any),
      Namespace.Shacl + "in",
      ModelDoc(ExternalModelVocabularies.Shacl, "in", "Enum constraint for the values of the property mapping"))

  val Unique: Field = Field(
      Bool,
      Namespace.Meta + "unique",
      ModelDoc(ModelVocabularies.Meta,
               "unique",
               "Marks the values for the property mapping as a primary key for this type of node")
  )

  val ExternallyLinkable: Field = Field(
      Bool,
      Namespace.Meta + "externallyLinkable",
      ModelDoc(ModelVocabularies.Meta, "linkable", "Marks this object property as supporting external links")
  )

}
