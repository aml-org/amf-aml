package amf.aml.internal.annotations

import amf.core.client.scala.model.domain.{AmfElement, Annotation, AnnotationGraphLoader, SerializableAnnotation}

case class RefInclude() extends SerializableAnnotation {
  override val name: String  = "ref-include"
  override val value: String = "true"
}

object RefInclude extends AnnotationGraphLoader {
  override def unparse(value: String, objects: Map[String, AmfElement]): Option[Annotation] = Some(RefInclude())
}
