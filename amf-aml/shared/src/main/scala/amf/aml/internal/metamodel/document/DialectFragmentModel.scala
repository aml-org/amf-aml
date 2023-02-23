package amf.aml.internal.metamodel.document

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.document.FragmentModel
import amf.core.internal.metamodel.domain.{ModelDoc, ModelVocabularies}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.document.DialectFragment

object DialectFragmentModel extends FragmentModel with ExternalContextModel {
  override def modelInstance: AmfObject = DialectFragment()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectFragment" :: FragmentModel.`type`

  override val fields: List[Field] = Externals :: Location :: FragmentModel.fields

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.Meta,
    "DialectFragment",
    "AML dialect mapping fragment that can be included in multiple AML dialects"
  )
}
