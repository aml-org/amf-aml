package amf.aml.internal.metamodel.document

import amf.aml.client.scala.model.document.DialectInstance
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Field
import com.github.ghik.silencer.silent
import amf.core.internal.metamodel.document._

object DialectInstanceModel extends DocumentModel with ExternalContextModel with DialectInstanceUnitModel {

  override def modelInstance: AmfObject = DialectInstance()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstance" :: DocumentModel.`type`

  @silent("deprecated")
  override val fields: List[Field] = DefinedBy :: GraphDependencies :: Externals :: DocumentModel.fields
}
