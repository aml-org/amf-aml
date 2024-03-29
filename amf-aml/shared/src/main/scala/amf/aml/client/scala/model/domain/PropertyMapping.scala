package amf.aml.client.scala.model.domain

import amf.core.client.scala.model._
import amf.core.client.scala.model.domain.{AmfScalar, DataNode}
import amf.core.client.scala.vocabulary.{Namespace, ValueType}
import amf.core.internal.metamodel.{Field, Type}
import amf.core.internal.parser.domain.{Annotations, Fields}
import amf.aml.internal.metamodel.domain.PropertyMappingModel._
import amf.aml.internal.metamodel.domain.{DialectDomainElementModel, PropertyMappingModel}
import amf.core.internal.metamodel.domain.ShapeModel
import org.yaml.model.YPart

class PropertyClassification
object ExtensionPointProperty       extends PropertyClassification
object LiteralProperty              extends PropertyClassification
object ObjectProperty               extends PropertyClassification
object ObjectPropertyCollection     extends PropertyClassification
object ObjectMapProperty            extends PropertyClassification
object ObjectMapInheritanceProperty extends PropertyClassification
object ObjectPairProperty           extends PropertyClassification
object LiteralPropertyCollection    extends PropertyClassification
object ExternalLinkProperty         extends PropertyClassification

case class PropertyMapping(fields: Fields, annotations: Annotations)
    extends PropertyLikeMapping[PropertyMappingModel.type]
    with MergeableMapping
    with NodeWithDiscriminator[PropertyMappingModel.type]
    with WithDefaultFacet {

  override def meta: PropertyMappingModel.type = PropertyMappingModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = ""
}

object PropertyMapping {
  def apply(): PropertyMapping = apply(Annotations())

  def apply(ast: YPart): PropertyMapping = apply(Annotations(ast))

  def apply(annotations: Annotations): PropertyMapping =
    PropertyMapping(Fields(), annotations)
}
