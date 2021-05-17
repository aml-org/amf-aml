package amf.plugins.document.vocabularies.annotations.serializable

import amf.core.annotations.serializable.SerializableAnnotations
import amf.core.model.domain.AnnotationGraphLoader
import amf.plugins.document.vocabularies.annotations.{
  AliasesLocation,
  CustomBase,
  CustomId,
  JsonPointerRef,
  RefInclude
}

private[amf] object AMLSerializableAnnotations extends SerializableAnnotations {

  override val annotations: Map[String, AnnotationGraphLoader] = Map(
      "aliases-location" -> AliasesLocation,
      "custom-id"        -> CustomId,
      "custom-base"      -> CustomBase,
      "ref-include"      -> RefInclude,
      "json-pointer-ref" -> JsonPointerRef
  )

}
