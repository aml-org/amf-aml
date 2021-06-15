package amf.aml.internal.annotations

import amf.core.client.scala.model.domain.{AmfElement, Annotation, AnnotationGraphLoader, SerializableAnnotation}

case class CustomId(uri: String = "true") extends SerializableAnnotation {
  override val name: String  = "custom-id"
  override val value: String = uri
}

object CustomId extends AnnotationGraphLoader {
  override def unparse(annotationValue: String, objects: Map[String, AmfElement]): Option[Annotation] =
    Some(CustomId(annotationValue))
}
