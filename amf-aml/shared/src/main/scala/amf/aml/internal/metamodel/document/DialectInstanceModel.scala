package amf.aml.internal.metamodel.document

import amf.aml.client.scala.model.document.DialectInstance
import amf.core.client.scala.model.domain.AmfObject
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.document._

import scala.annotation.nowarn

object DialectInstanceModel extends DocumentModel with ExternalContextModel with DialectInstanceUnitModel {

  override def modelInstance: AmfObject = DialectInstance()

  override val `type`: List[ValueType] =
    Namespace.Meta + "DialectInstance" :: DocumentModel.`type`

  @nowarn
  override val fields: List[Field] = DefinedBy :: GraphDependencies :: Externals :: DocumentModel.fields
}
