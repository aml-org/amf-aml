package amf.aml.internal.annotations

import amf.core.client.scala.model.domain.{AmfElement, Annotation, AnnotationGraphLoader, SerializableAnnotation}

case class CustomBase(uri: String = "true") extends SerializableAnnotation {
  override val name: String  = "custom-base"
  override val value: String = uri
}

object CustomBase extends AnnotationGraphLoader {
  override def unparse(annotationValue: String, objects: Map[String, AmfElement]): Option[Annotation] =
    Some(CustomBase(annotationValue))
}
