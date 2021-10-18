package amf.aml.client.scala.model.domain

import amf.core.client.scala.model.{DataType, StrField}
import amf.core.client.scala.model.domain.{DomainElement, Linkable}
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

  def domain(): StrField                               = fields.field(Domain)
  def withDomain(domainIri: String): AnnotationMapping = set(Domain, domainIri)

  override def meta: AnnotationMappingModel.type = AnnotationMappingModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  private[amf] override def componentId: String = s"annotation-mappings/${name.value()}"

  override def linkCopy(): Linkable = AnnotationMapping().withId(id)

  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement =
    AnnotationMapping.apply

  def toField: Field = {
    val iri = nodePropertyMapping()
      .option()
      .getOrElse((Namespace.Data + name.value()).iri())

    val propertyIdValue = ValueType(iri)
    val isObjectRange   = objectRange().nonEmpty || Option(typeDiscriminator()).isDefined

    if (isObjectRange) {
      if (allowMultiple().value() && sorted().value()) {
        Field(Type.SortedArray(DialectDomainElementModel()), propertyIdValue)
      } else if (allowMultiple().value()) {
        Field(Type.Array(DialectDomainElementModel()), propertyIdValue)
      } else {
        Field(DialectDomainElementModel(), propertyIdValue)
      }
    } else {
      val fieldType = literalRange().option() match {
        case Some(literal) if literal == (Namespace.Shapes + "link").iri() => Type.Iri
        case Some(literal) if literal == DataType.AnyUri =>
          Type.LiteralUri
        case Some(literal) if literal.endsWith("anyType") => Type.Any
        case Some(literal) if literal.endsWith("number")  => Type.Float
        case Some(literal) if literal == DataType.Integer => Type.Int
        case Some(literal) if literal == DataType.Float   => Type.Float
        case Some(literal) if literal == DataType.Double =>
          Type.Double
        case Some(literal) if literal == DataType.Boolean =>
          Type.Bool
        case Some(literal) if literal == DataType.Decimal => Type.Int
        case Some(literal) if literal == DataType.Time    => Type.Time
        case Some(literal) if literal == DataType.Date    => Type.Date
        case Some(literal) if literal == DataType.DateTime =>
          Type.Date
        case _ => Type.Str
      }

      if (allowMultiple().value()) {
        Field(Type.Array(fieldType), propertyIdValue)
      } else {
        Field(fieldType, propertyIdValue)
      }
    }
  }
}

object AnnotationMapping {
  def apply(): AnnotationMapping = apply(Annotations())

  def apply(ast: YMap): AnnotationMapping = apply(Annotations(ast))

  def apply(annotations: Annotations): AnnotationMapping = new AnnotationMapping(Fields(), annotations)

  def apply(fields: Fields, annotations: Annotations): AnnotationMapping = new AnnotationMapping(fields, annotations)
}
