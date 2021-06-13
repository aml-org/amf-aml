package amf.client.model.domain

import amf.client.convert.VocabulariesClientConverter._
import amf.core.client.platform.model.{AnyField, BoolField, DoubleField, IntField, StrField}
import amf.core.client.platform.model.domain._
import amf.plugins.document.vocabularies.model.domain.{AnnotationMapping => InternalAnnotationMapping}

import scala.collection.mutable
import scala.scalajs.js.annotation.{JSExportAll, JSExportTopLevel}

@JSExportAll
case class AnnotationMapping(override private[amf] val _internal: InternalAnnotationMapping) extends DomainElement {

  @JSExportTopLevel("model.domain.AnnotationMapping")
  def this() = this(InternalAnnotationMapping())

  def name(): StrField                    = _internal.name
  def nodePropertyMapping(): StrField     = _internal.nodePropertyMapping()
  def domain(): StrField                  = _internal.domain()
  def literalRange(): StrField            = _internal.literalRange()
  def objectRange(): ClientList[StrField] = _internal.objectRange().asClient
  def minCount(): IntField                = _internal.minCount()
  def pattern(): StrField                 = _internal.pattern()
  def minimum(): DoubleField              = _internal.minimum()
  def maximum(): DoubleField              = _internal.maximum()
  def allowMultiple(): BoolField          = _internal.allowMultiple()
  def enum(): ClientList[AnyField]        = _internal.enum().asClient
  def sorted(): BoolField                 = _internal.sorted()
  def typeDiscriminator(): ClientMap[String] = Option(_internal.typeDiscriminator()) match {
    case Some(m) =>
      m.foldLeft(mutable.Map[String, String]()) {
          case (acc, (k, v)) =>
            acc.put(k, v)
            acc
        }
        .asClient
    case None => mutable.Map[String, String]().asClient
  }
  def typeDiscriminatorName(): StrField = _internal.typeDiscriminatorName()
  def externallyLinkable(): BoolField   = _internal.externallyLinkable()

  def withMinCount(minCount: Int): AnnotationMapping = {
    _internal.withMinCount(minCount)
    this
  }
  def withPattern(pattern: String): AnnotationMapping = {
    _internal.withPattern(pattern)
    this
  }
  def withMinimum(min: Double): AnnotationMapping = {
    _internal.withMinimum(min)
    this
  }
  def withMaximum(max: Double): AnnotationMapping = {
    _internal.withMaximum(max)
    this
  }
  def withAllowMultiple(allow: Boolean): AnnotationMapping = {
    _internal.withAllowMultiple(allow)
    this
  }
  def withEnum(values: ClientList[Any]): AnnotationMapping = {
    _internal.withEnum(values.asInternal)
    this
  }
  def withSorted(sorted: Boolean): AnnotationMapping = {
    _internal.withSorted(sorted)
    this
  }

  def withTypeDiscriminator(typesMapping: ClientMap[String]): AnnotationMapping = {
    _internal.withTypeDiscriminator(typesMapping.asInternal)
    this
  }

  def withTypeDiscriminatorName(name: String): AnnotationMapping = {
    _internal.withTypeDiscriminatorName(name)
    this
  }

  def withExternallyLinkable(linkable: Boolean): _internal.type = _internal.withExternallyLinkable(linkable)

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
