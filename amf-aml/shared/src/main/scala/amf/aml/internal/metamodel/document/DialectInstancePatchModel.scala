package amf.aml.internal.metamodel.document

import amf.core.internal.metamodel.Field
import com.github.ghik.silencer.silent
import amf.core.internal.metamodel.document.{BaseUnitModel, DocumentModel, ExtensionLikeModel, FragmentModel}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.document.DialectInstancePatch

object DialectInstancePatchModel
    extends DocumentModel
    with ExternalContextModel
    with ExtensionLikeModel
    with DialectInstanceUnitModel {
  override def modelInstance: AmfObject = DialectInstancePatch()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstancePatch" :: Namespace.Document + "DocumentExtension" :: BaseUnitModel.`type`

  @silent("deprecated")
  override val fields: List[Field] = DefinedBy :: GraphDependencies :: Externals :: Extends :: FragmentModel.fields
}
