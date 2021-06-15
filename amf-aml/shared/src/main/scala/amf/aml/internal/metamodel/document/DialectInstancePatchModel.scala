package amf.aml.internal.metamodel.document

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Iri}
import amf.core.internal.metamodel.document.{BaseUnitModel, DocumentModel, ExtensionLikeModel, FragmentModel}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.document.DialectInstancePatch

object DialectInstancePatchModel extends DocumentModel with ExternalContextModel with ExtensionLikeModel {
  val DefinedBy: Field = Field(Iri, Namespace.Meta + "definedBy")

  val GraphDependencies: Field = Field(Array(Iri), Namespace.Document + "graphDependencies")

  override def modelInstance: AmfObject = DialectInstancePatch()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstancePatch" :: Namespace.Document + "DocumentExtension" :: BaseUnitModel.`type`

  override val fields: List[Field] = DefinedBy :: GraphDependencies :: Externals :: Extends :: FragmentModel.fields
}
