package amf.aml.client.scala.model.domain

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.domain.DomainElement
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.internal.utils.AmfStrings
import amf.aml.internal.metamodel.domain.ExternalModel
import amf.aml.internal.metamodel.domain.ExternalModel._
import org.yaml.model.{YMap, YMapEntry}

case class External(fields: Fields, annotations: Annotations) extends DomainElement {

  def alias: StrField = fields.field(DisplayName)
  def base: StrField  = fields.field(Base)

  def withAlias(alias: String, ann: Annotations = Annotations()): External = set(DisplayName, alias, ann)
  def withBase(base: String, ann: Annotations = Annotations()): External   = set(Base, base, ann)

  override def meta: ExternalModel.type = ExternalModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  private[amf] override def componentId: String = alias.option() match {
    case Some(alias) => "/externals/" + alias.urlComponentEncoded
    case None        => throw new Exception("Cannot set ID of external without alias")
  }
}

object External {

  def apply(): External = apply(Annotations())

  def apply(ast: YMapEntry): External = apply(Annotations(ast))

  def apply(annotations: Annotations): External = External(Fields(), annotations)
}
