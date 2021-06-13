package amf.plugins.features.validation.shacl.custom.validators

import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.domain.{AmfArray, AmfObject, AmfScalar, DomainElement}
import amf.core.internal.validation.core.{PropertyConstraint, ValidationSpecification}
import amf.plugins.features.validation.shacl.custom.PropertyConstraintValidator.extractPropertyValue
import amf.plugins.features.validation.shacl.custom.{PropertyConstraintValidator, ReportBuilder}

case object DataTypeConstraint extends PropertyConstraintValidator {

  private val supportedTypes = Set(DataType.String, DataType.Boolean, DataType.Integer, DataType.Double)

  override def canValidate(spec: PropertyConstraint): Boolean = spec.datatype.exists { datatype =>
    supportedTypes.contains(datatype)
  }

  override def validate(spec: ValidationSpecification,
                        propertyConstraint: PropertyConstraint,
                        parent: AmfObject,
                        reportBuilder: ReportBuilder): Unit = {
    propertyConstraint.datatype.foreach { datatype =>
      validateDataType(spec, propertyConstraint, parent, reportBuilder)
    }
  }

  private def validateDataType(validationSpecification: ValidationSpecification,
                               propertyConstraint: PropertyConstraint,
                               parentElement: AmfObject,
                               reportBuilder: ReportBuilder): Unit = {
    val xsdString  = DataType.String
    val xsdBoolean = DataType.Boolean
    val xsdInteger = DataType.Integer
    val xsdDouble  = DataType.Double
    extractPropertyValue(propertyConstraint, parentElement) match {
      case Some((_, element, _)) =>
        val elements = element match {
          case arr: AmfArray => arr.values
          case _             => Seq(element)
        }
        elements.foreach { element =>
          val maybeScalarValue = element match {
            case scalar: AmfScalar => Some(amfScalarToScala(scalar))
            case _                 => None
          }
          propertyConstraint.datatype match {
            case Some(s) if s == xsdString => // ignore

            case Some(s) if s == xsdBoolean =>
              maybeScalarValue match {
                case Some(_: Boolean) => // ignore
                case _ =>
                  reportBuilder.reportFailure(validationSpecification, propertyConstraint, parentElement.id)
              }
            case Some(s) if s == xsdInteger =>
              maybeScalarValue match {
                case Some(_: Integer) => // ignore
                case Some(_: Long)    => // ignore
                case _ =>
                  reportBuilder.reportFailure(validationSpecification, propertyConstraint, parentElement.id)
              }

            case Some(s) if s == xsdDouble =>
              maybeScalarValue match {
                case Some(_: Integer) => // ignore
                case Some(_: Long)    => // ignore
                case Some(_: Double)  => // ignore
                case Some(_: Float)   => // ignore
                case _ =>
                  reportBuilder.reportFailure(validationSpecification, propertyConstraint, parentElement.id)
              }

            case Some(other) =>
              throw new Exception(s"Data type '$other' for sh:datatype property constraint not supported yet")

            case _ => // ignore
          }
        }
      case _ => // ignore
    }
  }

  private def amfScalarToScala(scalar: AmfScalar): Any = PropertyConstraintValidator.amfScalarToScala(scalar)
}
