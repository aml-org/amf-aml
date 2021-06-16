package amf.aml.client.platform.model.domain

import amf.aml.internal.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.StrField
import amf.core.client.platform.model.domain.DomainElement
import amf.aml.client.scala.model.domain.{VocabularyReference => InternalVocabularyReference}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class VocabularyReference(override private[amf] val _internal: InternalVocabularyReference)
    extends DomainElement {

  @JSExportTopLevel("model.domain.VocabularyReference")
  def this() = this(InternalVocabularyReference())

  def alias: StrField     = _internal.alias
  def reference: StrField = _internal.reference

  def withAlias(alias: String): VocabularyReference         = _internal.withAlias(alias)
  def withReference(reference: String): VocabularyReference = _internal.withReference(reference)
}
