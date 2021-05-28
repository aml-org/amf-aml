package amf.plugins.document.vocabularies.model.domain

import amf.core.metamodel.Field
import amf.core.model.domain._
import amf.core.model.{BoolField, StrField}
import amf.core.parser.{Annotations, FieldEntry, Fields, Value}
import amf.core.vocabulary.ValueType
import amf.plugins.document.vocabularies.metamodel.domain.DialectDomainElementModel
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.{YMap, YMapEntry, YNode}

import scala.collection.mutable

class UnknownMapKeyProperty(val id: String) extends Exception

case class DialectDomainElement(override val fields: Fields, annotations: Annotations)
    extends DomainElement
    with Linkable {

  def objectCollectionProperty(f: Field): Seq[DialectDomainElement] = {
    fields
      .getValueAsOption(f)
      .collect({
        case Value(arr: AmfArray, _) =>
          arr.values.collect({ case d: DialectDomainElement => d })
      })
      .getOrElse(Nil)
  }

  def objectProperty(f: Field): Option[DialectDomainElement] =
    fields.getValueAsOption(f).collect({ case Value(d: DialectDomainElement, _) => d })

  def literalProperty(f: Field): Option[Any] =
    fields.getValueAsOption(f).collect({ case Value(v: AmfScalar, _) => v.value })

  def literalProperties(f: Field): Seq[Any] =
    fields
      .getValueAsOption(f)
      .collect({
        case Value(v: AmfArray, _) =>
          v.values.collect({ case s: AmfScalar => s.value })
      })
      .getOrElse(Nil)

  def fieldsToProperties: Iterable[(PropertyMapping, FieldEntry)] =
    fields
      .fields()
      .flatMap(fe => propertyForField(fe.field).map(pm => (pm, fe)))

  def propertyForField(f: Field): Option[PropertyMapping] =
    definedBy
      .propertiesMapping()
      .find(_.nodePropertyMapping().option().contains(f.toString))

  def isAbstract: BoolField =
    fields.field(DialectDomainElementModel.Abstract)
  def withAbstract(isAbstract: Boolean): DialectDomainElement = {
    set(DialectDomainElementModel.Abstract, isAbstract)
    this
  }

  def declarationName: StrField =
    fields.field(DialectDomainElementModel.DeclarationName)
  def withDeclarationName(name: String): DialectDomainElement = {
    set(DialectDomainElementModel.DeclarationName, name)
    this
  }

  // Types of the instance
  protected var instanceTypes: Seq[String] = Nil
  def withInstanceTypes(types: Seq[String]): DialectDomainElement = {
    instanceTypes = types
    this
  }

  // Dialect mapping defining the instance
  protected var instanceDefinedBy: Option[NodeMapping] = None
  def withDefinedBy(nodeMapping: NodeMapping): DialectDomainElement = {
    instanceDefinedBy = Some(nodeMapping)
    this
  }
  def definedBy: NodeMapping = instanceDefinedBy match {
    case Some(mapping) => mapping
    case None          => throw new Exception("NodeMapping for the instance not defined")
  }

  def localRefName: String = {
    if (isLink)
      linkTarget.map(_.id.split("#").last.split("/").last).getOrElse {
        throw new Exception(s"Cannot produce local reference without linked element at elem $id")
      } else id.split("#").last.split("/").last
  }

  def includeName: String = {
    if (isLink)
      linkLabel
        .option()
        .getOrElse(
            linkTarget
              .map(_.id.split("#").head)
              .getOrElse(throw new Exception(s"Cannot produce include reference without linked element at elem $id")))
    else
      throw new Exception(s"Cannot produce include reference without linked element at elem $id")
  }

  def iriToValue(iri: String) = ValueType(iri)

  def findPropertyByTermPropertyId(termPropertyId: String): String =
    findPropertyMappingByTermPropertyId(termPropertyId)
      .map(_.id)
      .getOrElse(termPropertyId)

  def findPropertyMappingByTermPropertyId(termPropertyId: String): Option[PropertyMapping] =
    definedBy
      .propertiesMapping()
      .find(_.nodePropertyMapping().value() == termPropertyId)

  def fieldForPropertyId(propertyId: String): Option[Field] =
    meta.fields.find(_.toString == propertyId)

  protected def propertyMappingForField(field: Field): Option[PropertyMapping] = {
    val iri = field.value.iri()
    definedBy.propertiesMapping().find(_.nodePropertyMapping().value() == iri)
  }

  def containsProperty(property: PropertyMapping): Boolean =
    fields.exists(property.toField)

  def setObjectField(property: PropertyMapping, value: DialectDomainElement, node: YNode): DialectDomainElement = {

    if (value.isUnresolved) {
      value.toFutureRef {
        case resolvedDialectDomainElement: DialectDomainElement =>
          val f = property.toField
          set(
              f,
              resolveUnreferencedLink(value.refName,
                                      value.annotations,
                                      resolvedDialectDomainElement,
                                      value.supportsRecursion.option().getOrElse(false))
                .withId(value.id),
              Annotations(node)
          )
        case resolved =>
          throw new Exception(s"Cannot resolve reference with not dialect domain element value ${resolved.id}")
      }
    } else {
      val f = property.toField
      set(f, value, Annotations(node))
    }

    this
  }

  def setObjectField(property: PropertyMapping, value: Seq[DialectDomainElement], node: YNode): DialectDomainElement = {
    val f = property.toField
    value match {
      case Nil if !fields.exists(f) => set(f, AmfArray(Nil, Annotations(node.value)), Annotations(node))
      case _ =>
        val (unresolved, normal) = value.partition({
          case l: Linkable if l.isUnresolved => true
          case _                             => false
        })
        set(f, AmfArray(normal), Annotations(node))
        unresolved.foreach {
          case linkable: Linkable if linkable.isUnresolved =>
            linkable.toFutureRef {
              case d: DialectDomainElement => setObjInCollection(f, node, d)
              case _                       => // ignore
            }
          case other => // ignore
        }
    }
    this
  }

  private def setObjInCollection(f: Field, node: YNode, newObj: DialectDomainElement) = {
    val objs: Seq[DialectDomainElement] = fields.field(f)
    set(f, AmfArray(objs :+ newObj), Annotations(node))
  }

  def setProperty(property: PropertyMapping, value: String, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  def setProperty(property: PropertyMapping, value: Int, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  def setProperty(property: PropertyMapping, value: SimpleDateTime, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  def setProperty(property: PropertyMapping, value: Boolean, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  def setProperty(property: PropertyMapping, value: Float, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  def setProperty(property: PropertyMapping, value: Double, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  def setProperty(property: PropertyMapping, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(entry.value, Annotations(entry.value)), Annotations(entry))
    this
  }

  def setProperty(property: PropertyMapping, value: Seq[Any], entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfArray(value.map(AmfScalar(_)), Annotations(entry.value)), Annotations(entry))
    this
  }

  override def meta: DialectDomainElementModel =
    if (instanceTypes.isEmpty) {
      DialectDomainElementModel()
    } else {
      new DialectDomainElementModel(instanceTypes.distinct,
                                    instanceDefinedBy.map(_.propertiesMapping().map(_.toField)).getOrElse(Seq.empty),
                                    instanceDefinedBy)
    }

  override def adopted(newId: String, cycle: Seq[String] = Seq()): DialectDomainElement.this.type =
    if (Option(this.id).isEmpty) simpleAdoption(newId) else this

  override def linkCopy(): Linkable =
    DialectDomainElement()
      .withId(id)
      .withDefinedBy(definedBy)
      .withInstanceTypes(instanceTypes)

  override def resolveUnreferencedLink[T](label: String,
                                          annotations: Annotations,
                                          unresolved: T,
                                          supportsRecursion: Boolean): T = {
    val unresolvedNodeMapping = unresolved.asInstanceOf[DialectDomainElement]
    val linked: T             = unresolvedNodeMapping.link(label, annotations)
    if (supportsRecursion && linked.isInstanceOf[Linkable])
      linked.asInstanceOf[Linkable].withSupportsRecursion(supportsRecursion)
    linked.asInstanceOf[DialectDomainElement].asInstanceOf[T]
  }

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = ""

  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement =
    DialectDomainElement.apply
}

object DialectDomainElement {
  def apply(): DialectDomainElement = apply(Annotations())

  def apply(ast: YMap): DialectDomainElement = apply(Annotations(ast))

  def apply(annotations: Annotations): DialectDomainElement =
    DialectDomainElement(Fields(), annotations)
}
