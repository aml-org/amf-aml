package amf.aml.internal.render.emitters.instances

import amf.aml.client.scala.model.domain.{DialectDomainElement, PropertyMapping}
import amf.core.client.common.position.Position
import amf.core.client.common.position.Position.ZERO
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfScalar}
import amf.core.internal.annotations.LexicalInformation
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.render.BaseEmitters.MapEntryEmitter
import amf.core.internal.render.emitters.EntryEmitter
import org.yaml.model.YDocument

case class ObjectPairEmitter(key: String,
                             array: AmfArray,
                             propertyMapping: PropertyMapping,
                             annotations: Option[Annotations] = None)
    extends EntryEmitter {
  override def emit(b: YDocument.EntryBuilder): Unit = {
    val keyProperty   = propertyMapping.mapTermKeyProperty().value()
    val valueProperty = propertyMapping.mapTermValueProperty().value()
    b.entry(
        key,
        _.obj { b =>
          val sortedElements = array.values.sortBy(elementPosition)
          sortedElements.foreach {
            case element: DialectDomainElement =>
              val keyField   = findFieldWithIri(keyProperty, element)
              val valueField = findFieldWithIri(valueProperty, element)
              if (keyField.isDefined && valueField.isDefined) {
                val keyLiteral =
                  element.fields.getValueAsOption(keyField.get).map(_.value)
                val valueLiteral = element.fields
                  .getValueAsOption(valueField.get)
                  .map(_.value)
                (keyLiteral, valueLiteral) match {
                  case (Some(keyScalar: AmfScalar), Some(valueScalar: AmfScalar)) =>
                    MapEntryEmitter(keyScalar.value.toString, valueScalar.value.toString).emit(b)
                  case _ =>
                    throw new Exception("Cannot generate object pair without scalar values for key and value")
                }
              } else {
                throw new Exception("Cannot generate object pair with undefined key or value")
              }
            case _ => // ignore
          }
        }
    )
  }

  private def findFieldWithIri(keyProperty: String, element: DialectDomainElement) = {
    element.meta.fields.find(_.value.iri() == keyProperty)
  }

  private def elementPosition(elem: AmfElement) = {
    elem.annotations
      .find(classOf[LexicalInformation])
      .map(_.range.start)
      .getOrElse(ZERO)
  }

  override def position(): Position = {
    annotations
      .flatMap(_.find(classOf[LexicalInformation]))
      .orElse(array.annotations.find(classOf[LexicalInformation]))
      .map(_.range.start)
      .getOrElse(ZERO)
  }
}
