package amf.plugins.document.vocabularies.model.document

import amf.core.model.StrField
import amf.core.model.document.BaseUnit

trait DialectInstanceUnit extends BaseUnit with ExternalContext[DialectInstanceUnit] {
  def references: Seq[BaseUnit]
  def graphDependencies: Seq[StrField]
  def definedBy(): StrField
}
