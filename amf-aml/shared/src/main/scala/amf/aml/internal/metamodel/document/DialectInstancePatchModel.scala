package amf.aml.internal.metamodel.document

import amf.aml.client.scala.model.document.DialectInstancePatch
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.document.{BaseUnitModel, DocumentModel, ExtensionLikeModel, FragmentModel}

import scala.annotation.nowarn

object DialectInstancePatchModel
    extends DocumentModel
    with ExternalContextModel
    with ExtensionLikeModel
    with DialectInstanceUnitModel {
  override def modelInstance: AmfObject = DialectInstancePatch()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstancePatch" :: Namespace.Document + "DocumentExtension" :: BaseUnitModel.`type`

  @nowarn
  override val fields: List[Field] = DefinedBy :: GraphDependencies :: Externals :: Extends :: FragmentModel.fields
}
