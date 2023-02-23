package amf.aml.internal.parse.dialects.property.like

import amf.core.client.scala.model.domain.AmfScalar
import amf.core.internal.parser.YMapOps
import amf.core.internal.parser.domain.{Annotations, ScalarNode}
import amf.aml.internal.metamodel.domain.PropertyLikeMappingModel
import amf.aml.client.scala.model.domain.PropertyLikeMapping
import amf.aml.internal.parse.dialects.DialectContext
import amf.core.internal.annotations.VirtualElement
import org.yaml.model.{YMap, YMapEntry}

import scala.language.implicitConversions

case class MandatoryParser(map: YMap, propertyLikeMapping: PropertyLikeMapping[_ <: PropertyLikeMappingModel])(implicit
    val ctx: DialectContext
) {
  def parse(): Unit = {

    // If I have minItems and mandatory ==> minCount = minItems & mandatory = mandatory
    // If I have only mandatory         ==> minCount = mandatory.toInt
    // If I have only minItems          ==> minCount = minItems & mandatory = false

    // This combined logic is to allow to validate empty arrays ([])

    val minItems        = parseMinItems
    val mandatory       = parseMandatory
    val existsMinItems  = minItems.nonEmpty
    val existsMandatory = mandatory.nonEmpty

    // If minItems key exists, it will always be saved in MinCount field
    if (existsMinItems && propertyLikeMapping.isMultiple) {
      val minItemsValue = minItems.get.value
      val minItemsEntry = minItems.get.entry
      propertyLikeMapping.set(
        propertyLikeMapping.meta.MinCount,
        AmfScalar(minItemsValue, Annotations(minItemsEntry.value)),
        Annotations(minItemsEntry)
      )
    }

    // Mandatory key will be processed based on the presence or not of minItems key
    if (existsMandatory) {
      val mandatoryValue = mandatory.get.value
      val mandatoryEntry = mandatory.get.entry
      // If mandatory and minItems keys exist, mandatory will be saved as a boolean in Mandatory field
      if (existsMinItems) {
        propertyLikeMapping.set(
          propertyLikeMapping.meta.Mandatory,
          AmfScalar(mandatoryValue.toBoolean, Annotations(mandatoryEntry.value)),
          Annotations(mandatoryEntry)
        )
      } else {
        // If mandatory key exists but minItems key not, mandatory will be saved in MinCount field (the original behavior)
        propertyLikeMapping.set(
          propertyLikeMapping.meta.MinCount,
          AmfScalar(mandatoryValue, Annotations(mandatoryEntry.value)),
          Annotations(mandatoryEntry)
        )
      }
    } else if (existsMinItems)
      propertyLikeMapping.set(propertyLikeMapping.meta.Mandatory, AmfScalar(false), Annotations(VirtualElement()))
    // If minItems key exists but mandatory key not, the field Mandatory will be set as false

  }

  private def parseMandatory: Option[ParsedEntry] = map.key("mandatory").map { entry =>
    val required = ScalarNode(entry.value).boolean().toBool
    val value    = if (required) 1 else 0
    ParsedEntry(value, entry)
  }

  private def parseMinItems: Option[ParsedEntry] = map.key("minItems").map { entry =>
    val value = ScalarNode(entry.value).integer().toNumber.intValue()
    ParsedEntry(value, entry)
  }

  private case class ParsedEntry(value: Int, entry: YMapEntry)

  class asBoolean(i: Int) {
    def toBoolean: Boolean = i == 1
  }

  implicit def convertIntToBoolean(i: Int): asBoolean = new asBoolean(i)

}
