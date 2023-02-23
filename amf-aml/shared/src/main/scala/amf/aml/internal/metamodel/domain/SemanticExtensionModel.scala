package amf.aml.internal.metamodel.domain

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Iri, Str}
import amf.core.internal.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.domain.SemanticExtension

/** Defines a the relation extension name -> extension mapping to be applied to target documents. An extension is
  * defined with an annotation mapping. Whenever we encounter an annotation in the target document (e.g. RAML API) we
  * check if the name of the annotation matches the ExtensionName fields value and if it does, we parse the value of
  * such annotation using the range of the ExtensionMappingDefinition. e.g. /myEndpoint: (extension-name):
  * extension-range (derived from extension mapping definition)
  */
object SemanticExtensionModel extends DomainElementModel {
  val ExtensionName: Field = Field(
    Str,
    Namespace.Core + "name",
    ModelDoc(ModelVocabularies.Core, "name", "Name that identifies an extension in the target document")
  )

  val ExtensionMappingDefinition: Field = Field(
    Iri,
    Namespace.Meta + "extensionMappingDefinition",
    ModelDoc(
      ModelVocabularies.Meta,
      "extensionMappingDefinition",
      "Extension mapping (annotation mapping) definition used to parse a certain extension identified with the ExtensionName"
    )
  )

  override def fields: List[Field] = ExtensionName :: ExtensionMappingDefinition :: DomainElementModel.fields

  override def modelInstance: SemanticExtension = SemanticExtension()

  override val `type`: List[ValueType] = Namespace.Meta + "ExtensionMapping" :: DomainElementModel.`type`

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.Meta,
    "SemanticExtension",
    "Mapping a particular extension name to an extension definition"
  )
}
