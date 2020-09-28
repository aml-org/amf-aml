package amf.plugins.document.vocabularies.metamodel.document

import amf.core.metamodel.Field
import amf.core.metamodel.document.FragmentModel
import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies.model.document.DialectFragment

object DialectFragmentModel extends FragmentModel with ExternalContextModel {
  override def modelInstance: AmfObject = DialectFragment()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectFragment" :: FragmentModel.`type`

  override val fields: List[Field] = Externals :: Location :: FragmentModel.fields

  override val doc: ModelDoc = ModelDoc(
      ModelVocabularies.Meta,
      "Dialect Fragment",
      "AML dialect mapping fragment that can be included in multiple AML dialects"
  )
}
