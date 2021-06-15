package amf.plugins.document.vocabularies.annotations

import amf.core.client.scala.model.domain.{AmfElement, Annotation, AnnotationGraphLoader, SerializableAnnotation}
import amf.plugins.document.vocabularies.model.domain.UnionNodeMapping

case class FromUnionNodeMapping(id: String) extends SerializableAnnotation {
  override val name: String  = "from-union-node-mapping"
  override val value: String = id
}

object FromUnionNodeMapping extends AnnotationGraphLoader {
  def apply(unionMapping: UnionNodeMapping): FromUnionNodeMapping =
    FromUnionNodeMapping(unionMapping.id)

  override def unparse(annotatedValue: String, objects: Map[String, AmfElement]): Option[Annotation] =
    Some(FromUnionNodeMapping(annotatedValue))
}
