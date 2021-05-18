package amf.client.model.domain

import amf.client.convert.VocabulariesClientConverter._
import amf.client.model._
import amf.plugins.document.vocabularies.model.domain.{AnnotationMapping => InternalAnnotationMapping}

import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class AnnotationMapping(override private[amf] val _internal: InternalAnnotationMapping) extends DomainElement {

  @JSExportTopLevel("model.domain.AnnotationMapping")
  def this() = this(InternalAnnotationMapping())

  def name(): StrField                    = _internal.name()
  def nodePropertyMapping(): StrField     = _internal.nodePropertyMapping()
  def domain(): StrField                  = _internal.domain()
  def literalRange(): StrField            = _internal.literalRange()
  def objectRange(): ClientList[StrField] = _internal.objectRange().asClient

  def withName(name: String): AnnotationMapping = {
    _internal.withName(name)
    this
  }

  def withNodePropertyMapping(propertyId: String): AnnotationMapping = {
    _internal.withNodePropertyMapping(propertyId)
    this
  }

  def withDomain(domainIri: String): AnnotationMapping = {
    _internal.withDomain(domainIri)
    this
  }

  def withLiteralRange(range: String): AnnotationMapping = {
    _internal.withLiteralRange(range)
    this
  }

  def withObjectRange(range: ClientList[String]): AnnotationMapping = {
    _internal.withObjectRange(range.asInternal)
    this
  }

}
