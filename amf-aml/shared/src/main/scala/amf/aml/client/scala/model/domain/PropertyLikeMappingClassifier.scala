package amf.aml.client.scala.model.domain

import amf.core.client.scala.vocabulary.Namespace

object PropertyLikeMappingClassifier {

  def classification(propertyLike: PropertyLikeMapping[_]): PropertyClassification = {
    val isAnyNode = propertyLike.objectRange().exists { obj =>
      obj.value() == (Namespace.Meta + "anyNode").iri()
    }
    val isLiteral           = propertyLike.literalRange().nonNull
    val isObject            = propertyLike.objectRange().nonEmpty
    val multiple            = propertyLike.allowMultiple().option().getOrElse(false)
    val (isMap, isMapValue) = hasMapTermValue(propertyLike)
    val isExternalLink      = propertyLike.externallyLinkable().option().getOrElse(false)

    if (isExternalLink)
      ExternalLinkProperty
    else if (isAnyNode)
      ExtensionPointProperty
    else if (isLiteral && !multiple)
      LiteralProperty
    else if (isLiteral)
      LiteralPropertyCollection
    else if (isObject && isMap && isMapValue)
      ObjectPairProperty
    else if (isObject && isMap)
      ObjectMapProperty
    else if (isObject && !multiple)
      ObjectProperty
    else
      ObjectPropertyCollection
  }

  private def hasMapTermValue(propertyLikeMapping: PropertyLikeMapping[_]): (Boolean, Boolean) = {
    propertyLikeMapping match {
      case mapping: PropertyMapping => (mapping.mapTermKeyProperty().nonNull, mapping.mapValueProperty().nonNull)
      case _                        => (false, false)
    }
  }
}
