package amf.aml.internal.validate

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{NodeMapping, PropertyMapping}
import amf.aml.internal.metamodel.domain.PropertyMappingModel
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.internal.metamodel.Field

case class AmlValidationCandidate(node: NodeMapping, mapping: PropertyMapping, enums: List[AmfScalar])

object LiteralCandidateCollector {

  def collect(dialect: Dialect): Seq[AmlValidationCandidate] = collectCandidates(dialect)

  private def collectCandidates(dialect: Dialect) = {
    dialect
      .iterator()
      .collect { case node: NodeMapping =>
        val properties = node.propertiesMapping().filter(_.`enum`().nonEmpty)
        properties.flatMap { mapping =>
          val scalarEnums = collectScalarValues(mapping, PropertyMappingModel.Enum)
          if (scalarEnums.nonEmpty) Some(AmlValidationCandidate(node, mapping, scalarEnums))
          else None
        }
      }
      .toList
      .flatten
  }

  private def collectScalarValues(mapping: PropertyMapping, field: Field): List[AmfScalar] =
    mapping.fields.get(field) match {
      case array: AmfArray => array.scalars.toList
      case _               => Nil
    }
}
