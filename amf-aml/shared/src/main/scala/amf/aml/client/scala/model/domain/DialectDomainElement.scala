package amf.aml.client.scala.model.domain

import amf.aml.internal.annotations.YNodeAnnotationOperations.getAnnotationsOf
import amf.aml.internal.metamodel.domain.DialectDomainElementModel
import amf.core.client.scala.model.domain._
import amf.core.client.scala.model.{BoolField, StrField}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.metamodel.Field
import amf.core.internal.parser.domain.{Annotations, Fields}
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.{YMap, YMapEntry, YNode}

class UnknownMapKeyProperty(val id: String) extends Exception

case class DialectDomainElement(override val fields: Fields, annotations: Annotations)
    extends DomainElement
    with Linkable {

  // Types of the instance
  private var instanceTypes: Seq[String] = Nil
  // Dialect mapping defining the instance
  private var instanceDefinedBy: Option[NodeMapping] = None

  def getObjectByProperty(iri: String): Seq[DialectDomainElement] =
    graph
      .getObjectByProperty(Namespace.defaultAliases.expand(iri).iri())
      .collect({ case d: DialectDomainElement => d })

  def getScalarByProperty(iri: String): Seq[Any] = graph.scalarByProperty(iri)

  def isAbstract: BoolField                                = fields.field(DialectDomainElementModel.Abstract)
  def declarationName: StrField                            = fields.field(DialectDomainElementModel.DeclarationName)
  def containsProperty(property: PropertyMapping): Boolean = graph.containsField(property.toField)

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

  def withAbstract(isAbstract: Boolean): DialectDomainElement = {
    set(DialectDomainElementModel.Abstract, isAbstract)
    this
  }

  def withDeclarationName(name: String): DialectDomainElement = {
    set(DialectDomainElementModel.DeclarationName, name)
    this
  }

  def withInstanceTypes(types: Seq[String]): DialectDomainElement = {
    instanceTypes = types
    this
  }

  def withDefinedBy(nodeMapping: NodeMapping): DialectDomainElement = {
    instanceDefinedBy = Some(nodeMapping)
    this
  }

  def withObjectProperty(iri: String, value: DialectDomainElement, node: YNode = YNode.Empty): DialectDomainElement = {
    findPropertyMappingByIri(Namespace.defaultAliases.expand(iri).iri()) match {
      case Some(mapping) =>
        withObjectField(mapping, value, Left(node))
        this
      case None =>
        throw new Exception(s"Cannot find node mapping for property IRI $iri")
    }
  }

  def withObjectCollectionProperty(propertyIri: String, value: Seq[DialectDomainElement]): this.type = {
    findPropertyMappingByIri(Namespace.defaultAliases.expand(propertyIri).iri()) match {
      case Some(mapping) =>
        withObjectCollectionProperty(mapping, value, Left(YNode.Empty))
        this
      case None =>
        throw new Exception(s"Cannot find node mapping for property IRI $propertyIri")
    }
  }

  def withLiteralProperty(propertyIri: String, value: String): this.type  = setLiteralPropertyBase(propertyIri, value)
  def withLiteralProperty(propertyIri: String, value: Int): this.type     = setLiteralPropertyBase(propertyIri, value)
  def withLiteralProperty(propertyIri: String, value: Double): this.type  = setLiteralPropertyBase(propertyIri, value)
  def withLiteralProperty(propertyIri: String, value: Float): this.type   = setLiteralPropertyBase(propertyIri, value)
  def withLiteralProperty(propertyIri: String, value: Boolean): this.type = setLiteralPropertyBase(propertyIri, value)
  def withLiteralProperty(propertyIri: String, dateTime: SimpleDateTime): this.type =
    setLiteralPropertyBase(propertyIri, dateTime)
  def withLiteralProperty(propertyIri: String, value: List[Any]): this.type =
    setLiteralPropertyBase(propertyIri, value)

  private[amf] def setObjectField(property: PropertyMapping,
                                  value: DialectDomainElement,
                                  node: YNode): DialectDomainElement =
    withObjectField(property, value, Left(node))

  private[amf] def withObjectField(property: PropertyMapping,
                                   value: DialectDomainElement,
                                   node: Either[YNode, YMapEntry]): DialectDomainElement = {
    val (annotations, _) = getAnnotationsOf(node)
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
              annotations
          )
        case resolved =>
          throw new Exception(s"Cannot resolve reference with not dialect domain element value ${resolved.id}")
      }
    } else {
      val f = property.toField
      set(f, value, annotations)
    }

    this
  }

  private[amf] def withObjectCollectionProperty(property: PropertyMapping,
                                                value: Seq[DialectDomainElement],
                                                node: YNode): DialectDomainElement =
    withObjectCollectionProperty(property, value, Left(node))

  private[amf] def withObjectCollectionProperty(property: PropertyMapping,
                                                value: Seq[DialectDomainElement],
                                                node: Either[YNode, YMapEntry]): DialectDomainElement = {
    val f                               = property.toField
    val (annotations, annotationsValue) = getAnnotationsOf(node)
    value match {
      case Nil if !fields.exists(f) => set(f, AmfArray(Nil, annotationsValue), annotations)
      case _ =>
        val (unresolved, normal) = value.partition({
          case l: Linkable if l.isUnresolved => true
          case _                             => false
        })
        set(f, AmfArray(normal), annotations)
        unresolved.foreach {
          case linkable: Linkable if linkable.isUnresolved =>
            linkable.toFutureRef {
              case d: DialectDomainElement => setObjInCollection(f, node, d)
              case _                       => // ignore
            }
          case _ => // ignore
        }
    }
    this
  }

  private[amf] def setProperty(property: PropertyMapping, value: Any, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  // TODO: WHY???
  private[amf] def setProperty(property: PropertyMapping, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(entry.value, Annotations(entry.value)), Annotations(entry))
    this
  }

  private[amf] def setProperty(property: PropertyMapping, value: Seq[Any], entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfArray(value.map(AmfScalar(_)), Annotations(entry.value)), Annotations(entry))
    this
  }

  private[amf] def objectCollectionProperty(f: Field): Seq[DialectDomainElement] = getObjectByProperty(f.value.iri())
  private[amf] def objectProperty(f: Field): Option[DialectDomainElement]        = objectCollectionProperty(f).headOption
  private[amf] def literalProperty(f: Field): Option[Any]                        = literalProperties(f).headOption
  private[amf] def literalProperties(f: Field): Seq[Any]                         = graph.scalarByField(f)

  private[amf] override def componentId: String = ""

  private def setObjInCollection(f: Field, node: Either[YNode, YMapEntry], newObj: DialectDomainElement) = {
    val (annotations, _)                = getAnnotationsOf(node)
    val objs: Seq[DialectDomainElement] = fields.field(f)
    set(f, AmfArray(objs :+ newObj), annotations)
  }

  private def findPropertyMappingByIri(propertyIri: String): Option[PropertyMapping] =
    definedBy
      .propertiesMapping()
      .find(_.nodePropertyMapping().value() == propertyIri)

  private def setLiteralPropertyBase(propertyIri: String, value: Any): this.type = {
    findPropertyMappingByIri(Namespace.defaultAliases.expand(propertyIri).iri()) match {
      case Some(mapping) if mapping.allowMultiple().is(true) =>
        value match {
          case seq: Seq[_] =>
            set(mapping.toField, AmfArray(seq.map(AmfScalar(_))))
          case other =>
            set(mapping.toField, AmfArray(Seq(AmfScalar(other))))
        }
      case Some(mapping) =>
        set(mapping.toField, AmfScalar(value))
        this
      case None =>
        throw new Exception(s"Cannot find node mapping for property IRI $propertyIri")
    }
  }

  override def meta: DialectDomainElementModel =
    if (instanceTypes.isEmpty) DialectDomainElementModel()
    else {
      new DialectDomainElementModel(instanceTypes.distinct,
                                    instanceDefinedBy.map(_.propertiesMapping().map(_.toField)).getOrElse(Seq.empty),
                                    instanceDefinedBy)
    }

  override def linkCopy(): DialectDomainElement = {
    DialectDomainElement()
      .withId(id)
      .withDefinedBy(definedBy)
      .withInstanceTypes(instanceTypes)
  }

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
  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement =
    DialectDomainElement.apply

  override def adopted(newId: String, cycle: Seq[String] = Seq()): DialectDomainElement.this.type =
    if (Option(this.id).isEmpty) simpleAdoption(newId) else this
}

object DialectDomainElement {
  def apply(): DialectDomainElement = apply(Annotations())

  def apply(ast: YMap): DialectDomainElement = apply(Annotations(ast))

  def apply(annotations: Annotations): DialectDomainElement =
    DialectDomainElement(Fields(), annotations)
}
