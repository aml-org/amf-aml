package amf.aml.internal.validate

import amf.core.client.common.validation.{ProfileName, SeverityLevels}
import amf.core.client.scala.model.DataType
import amf.core.client.scala.rdf.RdfModel
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.plugins.document.graph.JsonLdKeywords
import amf.core.internal.utils.AmfStrings
import amf.core.internal.validation.CoreValidations
import amf.core.internal.validation.core.{
  PropertyConstraint,
  SeverityMapping,
  ValidationProfile,
  ValidationSpecification
}
import amf.aml.internal.render.emitters.instances.{AmlEmittersHelper, NodeMappableFinder}
import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{NodeMappable, NodeMapping, PropertyMapping, UnionNodeMapping}
import amf.aml.internal.validate.AMFDialectValidations.staticValidations
import org.yaml.model.YDocument.EntryBuilder

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class AMFDialectValidations(val dialect: Dialect)(implicit val nodeMappableFinder: NodeMappableFinder)
    extends AmlEmittersHelper {

  def profile(): ValidationProfile = {
    val parsedValidations = validations()
    val severityMapping   = SeverityMapping().set(parsedValidations.map(_.name), SeverityLevels.VIOLATION)

    // TODO: dialect validation profile does not take into account severities of static validations
    ValidationProfile(
        name = ProfileName(dialect.nameAndVersion()),
        baseProfile = None,
        validations = parsedValidations ++ staticValidations,
        severities = severityMapping
    )
  }

  protected def validations(): Seq[ValidationSpecification] = {
    dialect.declares.flatMap {
      case nodeMapping: NodeMapping           => emitEntityValidations(nodeMapping, mutable.Set())
      case unionNodeMapping: UnionNodeMapping => emitRootUnionNodeMapping(unionNodeMapping, mutable.Set())
      case _                                  => None
    }
  }

  protected def emitEntityValidations(node: NodeMapping,
                                      recursion: mutable.Set[String]): List[ValidationSpecification] = {
    node
      .propertiesMapping()
      .flatMap { propertyMapping =>
        emitPropertyValidations(node, propertyMapping, recursion += node.id)
      }
      .toList
  }

  protected def emitRootUnionNodeMapping(nodeMapping: UnionNodeMapping,
                                         recursion: mutable.Set[String]): List[ValidationSpecification] = {
    val validations: ListBuffer[ValidationSpecification] = ListBuffer.empty
    val range                                            = nodeMapping.objectRange().map(_.value())
    range.foreach { rangeId =>
      findNodeMappingById(rangeId) match {
        case (_, nodeMapping: NodeMapping) =>
          validations ++= emitEntityValidations(nodeMapping, recursion += nodeMapping.id)
        case _ =>
        // ignore
      }
    }
    validations.toList
  }

  protected def emitPropertyValidations(node: NodeMapping,
                                        prop: PropertyMapping,
                                        recursion: mutable.Set[String]): List[ValidationSpecification] = {
    val validations: ListBuffer[ValidationSpecification] = ListBuffer.empty

    prop.minimum().option().foreach { minValue =>
      val message = s"Property '${prop.name()}' minimum inclusive value is $minValue"
      validations += new ValidationSpecification(
          name = validationId(node, prop.name().value(), "minimum"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Seq(node.id),
          propertyConstraints = Seq(PropertyConstraint(
              ramlPropertyId = prop.nodePropertyMapping().value(),
              name = validationId(node, prop.name().value(), "minimum") + "/prop",
              message = Some(message),
              minInclusive = Some(minValue.toString)
          ))
      )
    }

    prop.maximum().option().foreach { maxValue =>
      val message = s"Property '${prop.name()}' maximum inclusive value is $maxValue"
      validations += new ValidationSpecification(
          name = validationId(node, prop.name().value(), "maximum"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Seq(node.id),
          propertyConstraints = Seq(PropertyConstraint(
              ramlPropertyId = prop.nodePropertyMapping().value(),
              name = validationId(node, prop.name().value(), "maximum") + "/prop",
              message = Some(message),
              maxInclusive = Some(maxValue.toString)
          ))
      )
    }

    if (prop.minCount().nonNull && prop.minCount().value() > 0) {
      val message = s"Property '${prop.name()}' is mandatory"
      validations += new ValidationSpecification(
          name = validationId(node, prop.name().value(), "required"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Seq(node.id),
          propertyConstraints = Seq(
              PropertyConstraint(
                  ramlPropertyId = prop.nodePropertyMapping().value(),
                  name = validationId(node, prop.name().value(), "required") + "/prop",
                  message = Some(message),
                  minCount = Some("1")
              ))
      )
    }

    if (!prop.allowMultiple().value() && prop.mapTermKeyProperty().isNullOrEmpty) {
      val message = s"Property '${prop.name()}' cannot have more than 1 value"
      validations += new ValidationSpecification(
          name = validationId(node, prop.name().value(), "notCollection"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Seq(node.id),
          propertyConstraints = Seq(
              PropertyConstraint(
                  ramlPropertyId = prop.nodePropertyMapping().value(),
                  name = validationId(node, prop.name().value(), "notCollection") + "/prop",
                  message = Some(message),
                  maxCount = Some("1")
              ))
      )
    }

    prop.pattern().option() match {
      case Some(pattern) =>
        val message = s"Property '${prop.name()}' must match pattern $pattern"
        validations += new ValidationSpecification(
            name = validationId(node, prop.name().value(), "pattern"),
            message = message,
            ramlMessage = Some(message),
            oasMessage = Some(message),
            targetClass = Seq(node.id),
            propertyConstraints = Seq(
                PropertyConstraint(
                    ramlPropertyId = prop.nodePropertyMapping().value(),
                    name = validationId(node, prop.name().value(), "pattern") + "/prop",
                    message = Some(message),
                    pattern = Some(pattern)
                ))
        )
      case _ => // ignore
    }

    if (prop.enum().nonEmpty) {
      val values  = prop.enum().map(_.value())
      val message = s"Property '${prop.name()}' must match some value in ${values.mkString(",")}"
      validations += new ValidationSpecification(
          name = validationId(node, prop.name().value(), "enum"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Seq(node.id),
          propertyConstraints = Seq(
              PropertyConstraint(
                  ramlPropertyId = prop.nodePropertyMapping().value(),
                  name = validationId(node, prop.name().value(), "enum") + "/prop",
                  message = Some(message),
                  in = values.map { v =>
                    s"$v"
                  }
              ))
      )
    }

    // ranges here
    if (prop.literalRange().nonNull) {
      val dataRange = prop.literalRange().value()
      dataRange match {
        case DataType.Any =>
        // Ignore, AnyTypes ranges are not validated

        case DataType.Number | DataType.Float | DataType.Double =>
          val message = s"Property '${prop.name()}'  value must be of type ${DataType.Integer} or ${DataType.Float}"
          validations += new ValidationSpecification(
              name = validationId(node, prop.name().value(), "dialectRange"),
              message = message,
              ramlMessage = Some(message),
              oasMessage = Some(message),
              targetClass = Seq(node.id),
              propertyConstraints = Seq(
                  PropertyConstraint(
                      ramlPropertyId = prop.nodePropertyMapping().value(),
                      name = validationId(node, prop.name().value(), "dialectRange") + "/prop",
                      message = Some(message),
                      datatype = Some(DataType.Double)
                  ))
          )

        case DataType.Link =>
          val message = s"Property '${prop.name()}'  value must be of a link"
          validations += new ValidationSpecification(
              name = validationId(node, prop.name().value(), "dialectRange"),
              message = message,
              ramlMessage = Some(message),
              oasMessage = Some(message),
              targetClass = Seq(node.id),
              propertyConstraints = Seq(
                  PropertyConstraint(
                      ramlPropertyId = prop.nodePropertyMapping().value(),
                      name = validationId(node, prop.name().value(), "dialectRange") + "/prop",
                      message = Some(message),
                      datatype = Some(DataType.AnyUri)
                  ))
          )

        case literal if literal.endsWith("guid") =>
          val message = s"Property '${prop.name()}'  value must be of type xsd:string > $dataRange"
          validations += new ValidationSpecification(
              name = validationId(node, prop.name().value(), "dataRange"),
              message = message,
              ramlMessage = Some(message),
              oasMessage = Some(message),
              targetClass = Seq(node.id),
              propertyConstraints = Seq(
                  PropertyConstraint(
                      ramlPropertyId = prop.nodePropertyMapping().value(),
                      name = validationId(node, prop.name().value(), "dataRange") + "/prop",
                      message = Some(message),
                      datatype = Some((Namespace.Xsd + "string").iri())
                  ))
          )
        case literal =>
          val message = s"Property '${prop.name()}'  value must be of type $dataRange"
          validations += new ValidationSpecification(
              name = validationId(node, prop.name().value(), "dataRange"),
              message = message,
              ramlMessage = Some(message),
              oasMessage = Some(message),
              targetClass = Seq(node.id),
              propertyConstraints = Seq(
                  PropertyConstraint(
                      ramlPropertyId = prop.nodePropertyMapping().value(),
                      name = validationId(node, prop.name().value(), "dataRange") + "/prop",
                      message = Some(message),
                      datatype = Some(literal)
                  ))
          )

      }
    }

    if (prop.objectRange().nonEmpty &&
        !prop.objectRange().map(_.value()).contains((Namespace.Meta + "anyNode").iri()) &&
        !prop.externallyLinkable().option().getOrElse(false)) {

      val effectiveRange: Set[String] = prop
        .objectRange()
        .flatMap({ rangeId =>
          findNodeMappingById(rangeId.value()) match {
            case (_, nodeMapping: NodeMapping)       => Seq(nodeMapping.id)
            case (_, unionMapping: UnionNodeMapping) => unionMapping.objectRange().map(_.value())
            case _                                   => Seq.empty
          }
        })
        .toSet

      val message = s"Property '${prop.name()}'  value must be of type ${prop.objectRange()}"
      validations += new ValidationSpecification(
          name = validationId(node, prop.name().value(), "objectRange"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Seq(node.id),
          propertyConstraints = Seq(
              PropertyConstraint(
                  ramlPropertyId = prop.nodePropertyMapping().value(),
                  name = validationId(node, prop.name().value(), "objectRange") + "/prop",
                  message = Some(message),
                  `class` = effectiveRange.toSeq
              ))
      )
    }

    validations.toList
  }

  private def validationId(dialectNode: NodeMappable, propName: String, constraint: String): String =
    Option(dialectNode.id) match {
      case Some(id) => s"${id}_${propName.urlComponentEncoded}_${constraint}_validation"
      case None     => throw new Exception("Cannot generate validation for dialect node without ID")
    }

}

object AMFDialectValidations {
  type ConstraintSeverityOverrides = Map[String, Map[ProfileName, String]]

  val staticValidations: Seq[ValidationSpecification] = CoreValidations.validations ++ DialectValidations.validations
  val levels: ConstraintSeverityOverrides             = CoreValidations.levels ++ DialectValidations.levels
}