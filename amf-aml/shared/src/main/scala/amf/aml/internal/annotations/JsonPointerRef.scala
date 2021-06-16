package amf.aml.internal.annotations

import amf.core.client.scala.model.domain.{AmfElement, Annotation, AnnotationGraphLoader, SerializableAnnotation}

case class JsonPointerRef() extends SerializableAnnotation {
  override val name: String  = "json-pointer-ref"
  override val value: String = "true"
}

object JsonPointerRef extends AnnotationGraphLoader {
  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] = Some(JsonPointerRef())
}
