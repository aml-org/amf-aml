package amf.aml.internal.semantic

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.domain.extensions.CustomDomainProperty
import amf.core.internal.annotations.SourceAST
import org.mulesoft.lexer.SourceLocation
import org.yaml.model.YNode

trait AnnotationSchemaValidator {
  val annotationIndex: Map[String, CustomDomainProperty]
  def validate(name: String, key: YNode, eh: AMFErrorHandler): Unit
}

object IgnoreAnnotationSchemaValidator extends AnnotationSchemaValidator {
  override def validate(name: String, key: YNode, eh: AMFErrorHandler): Unit = {}

  override val annotationIndex: Map[String, CustomDomainProperty] = Map.empty
}
