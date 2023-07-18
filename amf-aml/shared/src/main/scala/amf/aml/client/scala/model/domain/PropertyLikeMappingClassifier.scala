package amf.aml.client.scala.model.domain

import amf.core.client.scala.vocabulary.Namespace

object PropertyLikeMappingClassifier {

  def classification(propertyLike: PropertyLikeMapping[_]): PropertyClassification = {
    val isAnyNode = propertyLike.objectRange().exists { obj =>
      obj.value() == (Namespace.Meta + "anyNode").iri()
    }
    val isLiteral      = propertyLike.literalRange().nonNull
    val isObject       = propertyLike.objectRange().nonEmpty
    val multiple       = propertyLike.allowMultiple().option().getOrElse(false)
    val hasMapKey_     = hasMapKey(propertyLike)
    val hasMapValue_   = hasMapValue(propertyLike)
    val isExternalLink = propertyLike.externallyLinkable().option().getOrElse(false)

    if (isExternalLink)
      ExternalLinkProperty
    else if (isAnyNode)
      ExtensionPointProperty
    else if (isLiteral && !multiple)
      LiteralProperty
    else if (isLiteral)
      LiteralPropertyCollection
    else if (isObject && hasMapKey_ && hasMapValue_)
      ObjectPairProperty
    else if (isObject && hasMapKey_)
      ObjectMapProperty
    else if (isObject && !multiple)
      ObjectProperty
    else
      ObjectPropertyCollection
  }

  private def hasMapKey(plm: PropertyLikeMapping[_]): Boolean = {
    plm.mapTermKeyProperty().nonNull || plm.mapKeyProperty().nonNull
  }

  private def hasMapValue(plm: PropertyLikeMapping[_]): Boolean = {
    plm.mapTermValueProperty().nonNull || plm.mapValueProperty().nonNull
  }
}
