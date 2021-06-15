package amf.aml.internal.annotations

import amf.core.client.scala.model.domain.Annotation

case class DiscriminatorField(key: String, value: String) extends Annotation
