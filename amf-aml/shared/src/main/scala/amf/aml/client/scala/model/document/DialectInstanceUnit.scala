package amf.aml.client.scala.model.document

import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.document.BaseUnit

trait DialectInstanceUnit extends BaseUnit with ExternalContext[DialectInstanceUnit] {
  def references: Seq[BaseUnit]
  def graphDependencies: Seq[StrField]
  def definedBy(): StrField
}
