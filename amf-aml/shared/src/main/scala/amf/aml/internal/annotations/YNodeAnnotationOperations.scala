package amf.aml.internal.annotations

import amf.core.internal.parser.domain.Annotations
import org.yaml.model.{YMapEntry, YNode}

object YNodeAnnotationOperations {

  def getAnnotationsOf(node: Either[YNode, YMapEntry]) = {
    val annotations = node match {
      case Left(value)  => Annotations(value)
      case Right(value) => Annotations(value)
    }
    val annotationsValue = node match {
      case Left(value)  => Annotations(value.value)
      case Right(value) => Annotations(value.value)
    }
    (annotations, annotationsValue)
  }
}
