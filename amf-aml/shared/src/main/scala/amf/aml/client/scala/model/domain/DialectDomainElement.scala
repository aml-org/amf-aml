package amf.aml.client.scala.model.domain

import amf.aml.internal.convert.VocabulariesClientConverter.ClientList
import amf.core.internal.metamodel.Field
import amf.core.client.scala.model.domain._
import amf.core.client.scala.model.{BoolField, StrField}
import amf.core.internal.parser.domain.{Annotations, FieldEntry, Fields, Value}
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.aml.internal.metamodel.domain.DialectDomainElementModel
import org.mulesoft.common.time.SimpleDateTime
import org.yaml.model.{YMap, YMapEntry, YNode}

class UnknownMapKeyProperty(val id: String) extends Exception

case class DialectDomainElement(override val fields: Fields, annotations: Annotations)
    extends DomainElement
    with Linkable {

  private[amf] def objectCollectionProperty(f: Field): Seq[DialectDomainElement] = getObjectByProperty(f.value.iri())

  private[amf] def objectProperty(f: Field): Option[DialectDomainElement] =
    objectCollectionProperty(f).headOption

  private[amf] def literalProperty(f: Field): Option[Any] =
    literalProperties(f).headOption

  private[amf] def literalProperties(f: Field): Seq[Any] = graph.scalarByField(f)

  private[amf] override def componentId: String = ""

  private def setObjInCollection(f: Field, node: Either[YNode, YMapEntry], newObj: DialectDomainElement) = {
    val annotations                     = annotationsFromEither(node)
    val objs: Seq[DialectDomainElement] = fields.field(f)
    set(f, AmfArray(objs :+ newObj), annotations)
  }

  private def annotationsFromEither(node: Either[YNode, YMapEntry]) =
    node match {
      case Left(value)  => Annotations(value)
      case Right(value) => Annotations(value)
    }

  private def findPropertyMappingByTermPropertyId(termPropertyId: String): Option[PropertyMapping] =
    definedBy
      .propertiesMapping()
      .find(_.nodePropertyMapping().value() == termPropertyId)

  // Types of the instance
  protected var instanceTypes: Seq[String] = Nil
  // Dialect mapping defining the instance
  protected var instanceDefinedBy: Option[NodeMapping] = None

  private[amf] def setProperty(property: PropertyMapping, value: String, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  private[amf] def setProperty(property: PropertyMapping, value: Int, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  private[amf] def setProperty(property: PropertyMapping,
                               value: SimpleDateTime,
                               entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  private[amf] def setProperty(property: PropertyMapping, value: Boolean, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  private[amf] def setProperty(property: PropertyMapping, value: Float, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  private[amf] def setProperty(property: PropertyMapping, value: Double, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(value, Annotations(entry.value)), Annotations(entry))
    this
  }

  private[amf] def setProperty(property: PropertyMapping, entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfScalar(entry.value, Annotations(entry.value)), Annotations(entry))
    this
  }

  private[amf] def setProperty(property: PropertyMapping, value: Seq[Any], entry: YMapEntry): DialectDomainElement = {
    set(property.toField, AmfArray(value.map(AmfScalar(_)), Annotations(entry.value)), Annotations(entry))
    this
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

  private def setLiteralPropertyBase(propertyId: String, value: Any): this.type = {
    findPropertyMappingByTermPropertyId(Namespace.defaultAliases.expand(propertyId).iri()) match {
      case Some(mapping) =>
        set(mapping.toField, AmfScalar(value))
        this
      case None =>
        throw new Exception(s"Cannot find node mapping for propertyId $propertyId")
    }
  }

  // Interface methods --------------------------------

  def getObjectByProperty(uri: String): Seq[DialectDomainElement] =
    graph
      .getObjectByProperty(Namespace.defaultAliases.expand(uri).iri())
      .collect({ case d: DialectDomainElement => d })

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

  def withInstanceTypes(types: Seq[String]): DialectDomainElement = {
    instanceTypes = types
    this
  }

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

  def containsProperty(property: PropertyMapping): Boolean = {
    graph.containsField(property.toField)
  }

  def setObjectProperty(uri: String, value: DialectDomainElement, node: YNode): DialectDomainElement = {
    findPropertyMappingByTermPropertyId(Namespace.defaultAliases.expand(uri).iri()) match {
      case Some(mapping) =>
        setObjectField(mapping, value, Left(node))
        this
      case None =>
        throw new Exception(s"Cannot find node mapping for propertyId $uri")
    }
  }

  def setObjectProperty(uri: String, value: DialectDomainElement): DialectDomainElement = {
    findPropertyMappingByTermPropertyId(Namespace.defaultAliases.expand(uri).iri()) match {
      case Some(mapping) =>
        setObjectField(mapping, value, Left(YNode.Empty))
        this
      case None =>
        throw new Exception(s"Cannot find node mapping for propertyId $uri")
    }
  }

  def setObjectCollectionProperty(propertyId: String, value: Seq[DialectDomainElement]): this.type = {
    findPropertyMappingByTermPropertyId(Namespace.defaultAliases.expand(propertyId).iri()) match {
      case Some(mapping) =>
        setObjectField(mapping, value, Left(YNode.Empty))
        this
      case None =>
        throw new Exception(s"Cannot find node mapping for propertyId $propertyId")
    }
  }

  private[amf] def setObjectField(property: PropertyMapping,
                                  value: DialectDomainElement,
                                  node: YNode): DialectDomainElement =
    setObjectField(property, value, Left(node))

  def setObjectField(property: PropertyMapping,
                     value: DialectDomainElement,
                     node: Either[YNode, YMapEntry]): DialectDomainElement = {
    val annotations = annotationsFromEither(node)
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

  private[amf] def setObjectField(property: PropertyMapping,
                                  value: Seq[DialectDomainElement],
                                  node: YNode): DialectDomainElement =
    setObjectField(property, value, Left(node))

  private[amf] def setObjectField(property: PropertyMapping,
                                  value: Seq[DialectDomainElement],
                                  node: Either[YNode, YMapEntry]): DialectDomainElement = {
    val f = property.toField
    val annotations = node match {
      case Left(value)  => Annotations(value)
      case Right(value) => Annotations(value)
    }
    val annotationsValue = node match {
      case Left(value)  => Annotations(value.value)
      case Right(value) => Annotations(value.value)
    }
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

  override def meta: DialectDomainElementModel =
    if (instanceTypes.isEmpty) {
      DialectDomainElementModel()
    } else {
      new DialectDomainElementModel(instanceTypes.distinct,
                                    instanceDefinedBy.map(_.propertiesMapping().map(_.toField)).getOrElse(Seq.empty),
                                    instanceDefinedBy)
    }

  override def linkCopy(): DialectDomainElement =
    DialectDomainElement()
      .withId(id)
      .withDefinedBy(definedBy)
      .withInstanceTypes(instanceTypes)

  def setLiteralProperty(propertyId: String, value: String): this.type = setLiteralPropertyBase(propertyId, value)

  def setLiteralProperty(propertyId: String, value: Int): this.type =
    setLiteralPropertyBase(propertyId, value)

  def setLiteralProperty(propertyId: String, value: Double): this.type = setLiteralPropertyBase(propertyId, value)

  def setLiteralProperty(propertyId: String, value: Float): this.type = setLiteralPropertyBase(propertyId, value)

  def setLiteralProperty(propertyId: String, value: Boolean): this.type = setLiteralPropertyBase(propertyId, value)

  def setLiteralProperty(propertyId: String, value: ClientList[Any]): this.type =
    setLiteralPropertyBase(propertyId, value)
}

object DialectDomainElement {
  def apply(): DialectDomainElement = apply(Annotations())

  def apply(ast: YMap): DialectDomainElement = apply(Annotations(ast))

  def apply(annotations: Annotations): DialectDomainElement =
    DialectDomainElement(Fields(), annotations)
}
