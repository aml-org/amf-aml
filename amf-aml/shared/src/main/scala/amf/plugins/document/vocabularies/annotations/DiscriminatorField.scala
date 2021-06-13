package amf.plugins.document.vocabularies.annotations

import amf.core.client.scala.model.domain.Annotation

case class DiscriminatorField(key: String, value: String) extends Annotation
