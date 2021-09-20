package amf.aml.internal.metamodel.document

import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.document.{DocumentModel, ModuleModel}
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.client.scala.model.document.DialectInstanceLibrary
import com.github.ghik.silencer.silent

object DialectInstanceLibraryModel extends DocumentModel with ExternalContextModel with DialectInstanceUnitModel {

  override def modelInstance: AmfObject = DialectInstanceLibrary()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstanceLibrary" :: ModuleModel.`type`

  @silent("deprecated")
  override val fields: List[Field] = DefinedBy :: GraphDependencies :: Externals :: ModuleModel.fields
}
