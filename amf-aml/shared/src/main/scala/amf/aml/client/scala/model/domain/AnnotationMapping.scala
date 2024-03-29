package amf.aml.client.scala.model.domain

import amf.core.client.scala.model.{DataType, StrField}
import amf.core.client.scala.model.domain.{AmfObject, DomainElement, Linkable}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.domain.{AnnotationMappingModel, DialectDomainElementModel}
import amf.aml.internal.metamodel.domain.AnnotationMappingModel._
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.{Field, Type}
import org.yaml.model.YMap

class AnnotationMapping(override val fields: Fields, override val annotations: Annotations)
    extends PropertyLikeMapping[AnnotationMappingModel.type]
    with NodeMappable[AnnotationMappingModel.type] {

  override def name: StrField                    = super[NodeMappable].name
  override def withName(name: String): this.type = set(meta.Name, name)

  def domain(): Seq[StrField]                               = fields.field(Domain)
  def withDomain(domainIri: Seq[String]): AnnotationMapping = set(Domain, domainIri)

  def appliesTo(element: AmfObject): Boolean = appliesTo(element.meta.`type`.map(_.iri()))

  def appliesTo(types: Seq[String]): Boolean = domain().flatMap(_.option()).exists(domain => types.contains(domain))

  override def meta: AnnotationMappingModel.type = AnnotationMappingModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = s"annotation-mappings/${name.value()}"

  override def linkCopy(): Linkable = AnnotationMapping().withId(id)

  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement =
    AnnotationMapping.apply
}

object AnnotationMapping {
  def apply(): AnnotationMapping = apply(Annotations())

  def apply(ast: YMap): AnnotationMapping = apply(Annotations(ast))

  def apply(annotations: Annotations): AnnotationMapping = new AnnotationMapping(Fields(), annotations)

  def apply(fields: Fields, annotations: Annotations): AnnotationMapping = new AnnotationMapping(fields, annotations)
}
