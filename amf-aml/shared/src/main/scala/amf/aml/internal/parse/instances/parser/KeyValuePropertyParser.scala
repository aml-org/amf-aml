package amf.aml.internal.parse.instances.parser

import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMapping, PropertyMapping, UnknownMapKeyProperty}
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.parse.instances.DialectInstanceParser.typesFrom
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.client.scala.vocabulary.ValueType
import amf.core.internal.metamodel.Field
import amf.core.internal.metamodel.Type.{Array, Str}
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.utils.AmfStrings
import org.yaml.model.{YMap, YMapEntry, YScalar, YSequence}

object KeyValuePropertyParser {

  def parse(id: String, propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement)(
      implicit ctx: DialectInstanceContext): Unit = {
    val propertyKeyMapping   = property.mapTermKeyProperty().option()
    val propertyValueMapping = property.mapTermValueProperty().option()
    if (propertyKeyMapping.isDefined && propertyValueMapping.isDefined) {
      val nested = ctx.dialect.declares.find(_.id == property.objectRange().head.value()) match {
        case Some(nodeMapping: NodeMapping) =>
          propertyEntry.value.as[YMap].entries flatMap { pair: YMapEntry =>
            val nestedId = id + "/" + propertyEntry.key.as[YScalar].text.urlComponentEncoded + "/" + pair.key
              .as[YScalar]
              .text
              .urlComponentEncoded
            val effectiveTypes      = typesFrom(nodeMapping)
            val valueAllowsMultiple = extractAllowMultipleForProp(propertyValueMapping, nodeMapping).getOrElse(false)
            val nestedNode = DialectDomainElement(Annotations(pair))
              .withId(nestedId)
              .withDefinedBy(nodeMapping)
              .withInstanceTypes(effectiveTypes)
            try {
              nestedNode.set(Field(Str, ValueType(propertyKeyMapping.get)),
                             AmfScalar(pair.key.as[YScalar].text),
                             Annotations(pair.key))

              if (valueAllowsMultiple) {
                pair.value.value match {
                  case seq: YSequence =>
                    nestedNode.set(
                        Field(Array(Str), ValueType(propertyValueMapping.get)),
                        AmfArray(seq.nodes.flatMap(_.asScalar).map(AmfScalar(_)), Annotations(seq)),
                        Annotations(pair.value)
                    )
                  case scalar: YScalar =>
                    nestedNode.set(Field(Array(Str), ValueType(propertyValueMapping.get)),
                                   AmfArray(Seq(AmfScalar(scalar.text))),
                                   Annotations(pair.value))
                  case _ => // ignore
                }
              } else {
                nestedNode.set(Field(Str, ValueType(propertyValueMapping.get)),
                               AmfScalar(pair.value.as[YScalar].text),
                               Annotations(pair.value))
              }
            } catch {
              case e: UnknownMapKeyProperty =>
                ctx.eh.violation(DialectError,
                                 e.id,
                                 s"Cannot find mapping for key map property ${e.id}",
                                 pair.location)
            }
            Some(nestedNode)
          }
        case _ =>
          ctx.eh.violation(
              DialectError,
              id,
              s"Cannot find mapping for property range of mapValue property: ${property.objectRange().head.value()}",
              propertyEntry.location
          )
          Nil
      }

      node.withObjectCollectionProperty(property, nested, Left(propertyEntry.key))

    } else {
      ctx.eh.violation(DialectError,
                       id,
                       s"Both 'mapKey' and 'mapValue' are mandatory in a map pair property mapping",
                       propertyEntry.location)
    }
  }

  private def extractAllowMultipleForProp(propertyValueMapping: Option[String], nodeMapping: NodeMapping) = {
    nodeMapping
      .propertiesMapping()
      .find(_.nodePropertyMapping().option().contains(propertyValueMapping.get))
      .flatMap(_.allowMultiple().option())
  }
}
