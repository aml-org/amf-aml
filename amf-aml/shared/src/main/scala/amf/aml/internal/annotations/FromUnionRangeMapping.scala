package amf.aml.internal.annotations

import amf.core.client.scala.model.domain.Annotation

/*
  If AMF can't determine a single range for the node this annotation is added to
  distinguish which ranges where analyzed (ALS uses this to provide suggestions)
 */
case class FromUnionRangeMapping(possibleRanges: Seq[String]) extends Annotation