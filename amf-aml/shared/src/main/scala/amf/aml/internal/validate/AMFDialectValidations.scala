package amf.aml.internal.validate

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.NodeMappable.AnyNodeMappable
import amf.aml.client.scala.model.domain._
import amf.aml.internal.render.emitters.instances.{AmlEmittersHelper, DialectIndex, NodeMappableFinder}
import amf.aml.internal.validate.AMFDialectValidations.staticValidations
import amf.core.client.common.validation.{ProfileName, SeverityLevels}
import amf.core.client.scala.model.{DataType, StrField}
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.utils.AmfStrings
import amf.core.internal.validation.CoreValidations
import amf.core.internal.validation.core.{
  PropertyConstraint,
  SeverityMapping,
  ValidationProfile,
  ValidationSpecification
}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class AMFDialectValidations(val dialect: Dialect)(implicit val nodeMappableFinder: NodeMappableFinder) {

  private val index = DialectIndex(dialect, nodeMappableFinder)

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

  def propertyValidations(mapping: NodeMapping): Seq[ValidationSpecification] = {
    mapping.propertiesMapping().flatMap(emitPropertyValidations(mapping, _))
  }

  protected def validations(): Seq[ValidationSpecification] = {
    val mappingValidations = dialect.declares.flatMap {
      case nodeMapping: NodeMapping           => emitEntityValidations(nodeMapping, mutable.Set())
      case unionNodeMapping: UnionNodeMapping => emitRootUnionNodeMapping(unionNodeMapping, mutable.Set())
      case _                                  => None
    }
    mappingValidations ++ dialect.extensions().flatMap(emitExtensionValidations)
  }

  protected def emitExtensionValidations(extension: SemanticExtension): List[ValidationSpecification] = {
    lookupAnnotation(extension).toList
      .flatMap { mapping =>
        val targets = mapping.domain().flatMap(_.option())
        targets.flatMap { classTarget =>
          emitPropertyValidations(mapping, mapping, Option(extension.extensionName()), Some(classTarget))
        }
      }
  }

  protected def lookupAnnotation(extension: SemanticExtension): Option[AnnotationMapping] = {
    dialect.declares.collectFirst {
      case mapping: AnnotationMapping if mapping.id == extension.extensionMappingDefinition().value() => mapping
    }
  }

  protected def emitEntityValidations(node: NodeMapping,
                                      recursion: mutable.Set[String]): List[ValidationSpecification] = {
    node
      .propertiesMapping()
      .flatMap { propertyMapping =>
        emitPropertyValidations(node, propertyMapping)
      }
      .toList
  }

  protected def emitRootUnionNodeMapping(nodeMapping: UnionNodeMapping,
                                         recursion: mutable.Set[String]): List[ValidationSpecification] = {
    val validations: ListBuffer[ValidationSpecification] = ListBuffer.empty
    val range                                            = nodeMapping.objectRange().map(_.value())
    range.foreach { rangeId =>
      index.findNodeMappingById(rangeId) match {
        case (_, nodeMapping: NodeMapping) =>
          validations ++= emitEntityValidations(nodeMapping, recursion += nodeMapping.id)
        case _ =>
        // ignore
      }
    }
    validations.toList
  }

  protected def emitPropertyValidations(node: AnyNodeMappable,
                                        prop: PropertyLikeMapping[_],
                                        propertyName: Option[StrField] = None,
                                        targetClass: Option[String] = None): List[ValidationSpecification] = {
    val validations: ListBuffer[ValidationSpecification] = ListBuffer.empty

    val finalTargetClass        = targetClass.getOrElse(node.id)
    val finalPropName: StrField = propertyName.getOrElse(prop.name())
    prop.minimum().option().foreach { minValue =>
      val message = s"Property '${finalPropName}' minimum inclusive value is $minValue"
      validations += new ValidationSpecification(
          name = validationId(node, finalPropName.value(), "minimum"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Set(finalTargetClass),
          propertyConstraints = Seq(PropertyConstraint(
              ramlPropertyId = prop.nodePropertyMapping().value(),
              name = validationId(node, finalPropName.value(), "minimum") + "/prop",
              message = Some(message),
              minInclusive = Some(minValue.toString)
          ))
      )
    }

    prop.maximum().option().foreach { maxValue =>
      val message = s"Property '${finalPropName}' maximum inclusive value is $maxValue"
      validations += new ValidationSpecification(
          name = validationId(node, finalPropName.value(), "maximum"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Set(finalTargetClass),
          propertyConstraints = Seq(PropertyConstraint(
              ramlPropertyId = prop.nodePropertyMapping().value(),
              name = validationId(node, finalPropName.value(), "maximum") + "/prop",
              message = Some(message),
              maxInclusive = Some(maxValue.toString)
          ))
      )
    }

    // Mandatory field will be validated along with minCount
    if (prop.minCount().nonNull) {
      val message = s"Property '${finalPropName}' is mandatory"
      validations += new ValidationSpecification(
          name = validationId(node, finalPropName.value(), "required"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Set(finalTargetClass),
          propertyConstraints = Seq(
              PropertyConstraint(
                  ramlPropertyId = prop.nodePropertyMapping().value(),
                  name = validationId(node, finalPropName.value(), "required") + "/prop",
                  message = Some(message),
                  minCount = prop.minCount().option().map(_.toString),
                  mandatory = prop.mandatory().option().map(_.toString)
              ))
      )
    }

    if (!prop.allowMultiple().value() && prop
          .isInstanceOf[PropertyMapping] && prop.asInstanceOf[PropertyMapping].mapTermKeyProperty().isNullOrEmpty) {
      val message = s"Property '${finalPropName}' cannot have more than 1 value"
      validations += new ValidationSpecification(
          name = validationId(node, finalPropName.value(), "notCollection"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Set(finalTargetClass),
          propertyConstraints = Seq(
              PropertyConstraint(
                  ramlPropertyId = prop.nodePropertyMapping().value(),
                  name = validationId(node, finalPropName.value(), "notCollection") + "/prop",
                  message = Some(message),
                  maxCount = Some("1")
              ))
      )
    }

    prop.pattern().option() match {
      case Some(pattern) =>
        val message = s"Property '${finalPropName}' must match pattern $pattern"
        validations += new ValidationSpecification(
            name = validationId(node, finalPropName.value(), "pattern"),
            message = message,
            ramlMessage = Some(message),
            oasMessage = Some(message),
            targetClass = Set(finalTargetClass),
            propertyConstraints = Seq(
                PropertyConstraint(
                    ramlPropertyId = prop.nodePropertyMapping().value(),
                    name = validationId(node, finalPropName.value(), "pattern") + "/prop",
                    message = Some(message),
                    pattern = Some(pattern)
                ))
        )
      case _ => // ignore
    }

    if (prop.enum().nonEmpty) {
      val values  = prop.enum().map(_.value())
      val message = s"Property '${finalPropName}' must match some value in ${values.mkString(",")}"
      validations += new ValidationSpecification(
          name = validationId(node, finalPropName.value(), "enum"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Set(finalTargetClass),
          propertyConstraints = Seq(
              PropertyConstraint(
                  ramlPropertyId = prop.nodePropertyMapping().value(),
                  name = validationId(node, finalPropName.value(), "enum") + "/prop",
                  message = Some(message),
                  in = values.toSet
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
          val message = s"Property '${finalPropName}'  value must be of type ${DataType.Integer} or ${DataType.Float}"
          validations += new ValidationSpecification(
              name = validationId(node, finalPropName.value(), "dialectRange"),
              message = message,
              ramlMessage = Some(message),
              oasMessage = Some(message),
              targetClass = Set(finalTargetClass),
              propertyConstraints = Seq(
                  PropertyConstraint(
                      ramlPropertyId = prop.nodePropertyMapping().value(),
                      name = validationId(node, finalPropName.value(), "dialectRange") + "/prop",
                      message = Some(message),
                      datatype = Some(DataType.Double)
                  ))
          )

        case DataType.Link =>
          val message = s"Property '${finalPropName}'  value must be of a link"
          validations += new ValidationSpecification(
              name = validationId(node, finalPropName.value(), "dialectRange"),
              message = message,
              ramlMessage = Some(message),
              oasMessage = Some(message),
              targetClass = Set(finalTargetClass),
              propertyConstraints = Seq(
                  PropertyConstraint(
                      ramlPropertyId = prop.nodePropertyMapping().value(),
                      name = validationId(node, finalPropName.value(), "dialectRange") + "/prop",
                      message = Some(message),
                      datatype = Some(DataType.AnyUri)
                  ))
          )

        case literal if literal.endsWith("guid") =>
          val message = s"Property '${finalPropName}'  value must be of type xsd:string > $dataRange"
          validations += new ValidationSpecification(
              name = validationId(node, finalPropName.value(), "dataRange"),
              message = message,
              ramlMessage = Some(message),
              oasMessage = Some(message),
              targetClass = Set(finalTargetClass),
              propertyConstraints = Seq(
                  PropertyConstraint(
                      ramlPropertyId = prop.nodePropertyMapping().value(),
                      name = validationId(node, finalPropName.value(), "dataRange") + "/prop",
                      message = Some(message),
                      datatype = Some((Namespace.Xsd + "string").iri())
                  ))
          )
        case literal =>
          val message = s"Property '${finalPropName}'  value must be of type $dataRange"
          validations += new ValidationSpecification(
              name = validationId(node, finalPropName.value(), "dataRange"),
              message = message,
              ramlMessage = Some(message),
              oasMessage = Some(message),
              targetClass = Set(finalTargetClass),
              propertyConstraints = Seq(
                  PropertyConstraint(
                      ramlPropertyId = prop.nodePropertyMapping().value(),
                      name = validationId(node, finalPropName.value(), "dataRange") + "/prop",
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
          index.findNodeMappingById(rangeId.value()) match {
            case (_, nodeMapping: NodeMapping)       => Seq(nodeMapping.id)
            case (_, unionMapping: UnionNodeMapping) => unionMapping.objectRange().map(_.value())
            case _                                   => Seq.empty
          }
        })
        .toSet

      val message = s"Property '${finalPropName}'  value must be of type ${prop.objectRange()}"
      validations += new ValidationSpecification(
          name = validationId(node, finalPropName.value(), "objectRange"),
          message = message,
          ramlMessage = Some(message),
          oasMessage = Some(message),
          targetClass = Set(finalTargetClass),
          propertyConstraints = Seq(
              PropertyConstraint(
                  ramlPropertyId = prop.nodePropertyMapping().value(),
                  name = validationId(node, finalPropName.value(), "objectRange") + "/prop",
                  message = Some(message),
                  `class` = effectiveRange.toSeq
              ))
      )
    }

    validations.toList
  }

  private def validationId(dialectNode: AnyNodeMappable, propName: String, constraint: String): String =
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
