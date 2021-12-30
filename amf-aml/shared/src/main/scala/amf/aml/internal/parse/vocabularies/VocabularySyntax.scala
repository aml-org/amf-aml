package amf.aml.internal.parse.vocabularies
import org.yaml.model.{IllegalTypeHandler, YMap, YScalar}

trait VocabularySyntax { this: VocabularyContext =>

  val vocabulary: Map[String, String] = Map(
      "$type"         -> "string",
      "base"          -> "string",
      "usage"         -> "string",
      "vocabulary"    -> "string",
      "uses"          -> "libraries",
      "external"      -> "libraries",
      "classTerms"    -> "ClassTerm[]",
      "propertyTerms" -> "PropertyTerm[]"
  )

  val classTerm: Map[String, String] = Map(
      "displayName" -> "string",
      "description" -> "string",
      "properties"  -> "string[]",
      "extends"     -> "string[]"
  )

  val propertyTerm: Map[String, String] = Map(
      "displayName" -> "string",
      "description" -> "string",
      "range"       -> "string[]",
      "extends"     -> "string[]"
  )

  def closedNode(nodeType: String, id: String, map: YMap)(implicit errorHandler: IllegalTypeHandler): Unit = {
    val allowedProps = nodeType match {
      case "vocabulary"   => vocabulary
      case "classTerm"    => classTerm
      case "propertyTerm" => propertyTerm
    }
    map.map.keySet.map(_.as[YScalar].text).foreach { property =>
      allowedProps.get(property) match {
        case Some(_) => // correct
        case None    => closedNodeViolation(id, property, nodeType, map)
      }
    }
  }
}
