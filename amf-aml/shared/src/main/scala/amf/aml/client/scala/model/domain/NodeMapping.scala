package amf.aml.client.scala.model.domain

import amf.aml.internal.metamodel.domain.NodeMappingModel
import amf.aml.internal.metamodel.domain.NodeMappingModel._
import amf.core.client.scala.model.domain.{DomainElement, Linkable}
import amf.core.client.scala.model.{BoolField, StrField}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.core.internal.utils._
import org.yaml.model.YMap

class NodeMapping(override val fields: Fields, override val annotations: Annotations)
    extends AnyMapping(fields)
    with Linkable
    with MergeableMapping
    with NodeMappable[NodeMappingModel.type] {

  override def meta: NodeMappingModel.type = NodeMappingModel

  def nodetypeMapping: StrField                 = fields.field(NodeTypeMapping)
  def propertiesMapping(): Seq[PropertyMapping] = fields.field(PropertiesMapping)
  def idTemplate: StrField                      = fields.field(IdTemplate)
  def closed: BoolField                         = fields.field(Closed)
  def resolvedExtends: Seq[String]              = fields.field(ResolvedExtends)

  def withNodeTypeMapping(nodeType: String): NodeMapping              = set(NodeTypeMapping, nodeType)
  def withPropertiesMapping(props: Seq[PropertyMapping]): NodeMapping = setArrayWithoutId(PropertiesMapping, props)
  def withClosed(value: Boolean): NodeMapping                         = set(Closed, value)
  def withIdTemplate(idTemplate: String): NodeMapping                 = set(IdTemplate, idTemplate)
  def withResolvedExtends(ids: Seq[String]): NodeMapping              = set(ResolvedExtends, ids)

  /**
    * Returns the properties forming the primary key for this node.
    * Properties are already sorted.
    */
  def primaryKey(): Seq[PropertyMapping] =
    propertiesMapping()
      .filter(_.unique().option().getOrElse(false))
      .sortBy(_.nodePropertyMapping().value())

  override def linkCopy(): NodeMapping = NodeMapping().withId(id)

  override def resolveUnreferencedLink[T](label: String,
                                          annotations: Annotations,
                                          unresolved: T,
                                          supportsRecursion: Boolean): T = {
    val unresolvedNodeMapping = unresolved.asInstanceOf[NodeMapping]
    val linked: T             = link(label, annotations)
    if (supportsRecursion && linked.isInstanceOf[Linkable])
      linked.asInstanceOf[Linkable].withSupportsRecursion(supportsRecursion)
    linked
      .asInstanceOf[NodeMapping]
      .withId(unresolvedNodeMapping.id)
      .withName(unresolvedNodeMapping.name.value())
      .asInstanceOf[T]
  }

  /** Value , path + field value that is used to compose the id when the object its adopted */
  private[amf] override def componentId: String = {
    "/" + name.value().urlComponentEncoded
  }

  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement = NodeMapping.apply
}

object NodeMapping {
  def apply(): NodeMapping = apply(Annotations())

  def apply(ast: YMap): NodeMapping = apply(Annotations(ast))

  def apply(annotations: Annotations): NodeMapping = NodeMapping(Fields(), annotations)

  def apply(fields: Fields, annotations: Annotations): NodeMapping = new NodeMapping(fields, annotations)

}
