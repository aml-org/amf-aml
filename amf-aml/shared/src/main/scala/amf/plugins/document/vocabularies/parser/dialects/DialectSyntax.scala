package amf.plugins.document.vocabularies.parser.dialects

import org.yaml.model.{IllegalTypeHandler, YMap, YNode, YScalar, YType}

trait DialectSyntax { this: DialectContext =>
  val dialect: Map[String, Boolean] = Map(
      "$dialect"     -> false,
      "dialect"      -> true,
      "version"      -> true,
      "usage"        -> false,
      "external"     -> false,
      "uses"         -> false,
      "nodeMappings" -> false,
      "documents"    -> false,
      "annotationMappings" -> false
  )

  val library: Map[String, Boolean] = Map(
      "usage"        -> false,
      "external"     -> false,
      "uses"         -> false,
      "nodeMappings" -> false
  )

  val nodeMapping: Map[String, Boolean] = Map(
      "classTerm"  -> false,
      "mapping"    -> false,
      "idProperty" -> false,
      "idTemplate" -> false,
      "patch"      -> false,
      "extends"    -> false
  )

  val fragment: Map[String, Boolean] = Map(
      "usage"    -> false,
      "external" -> false,
      "uses"     -> false
  ) ++ nodeMapping

  val propertyMapping: Map[String, Boolean] = Map(
      "propertyTerm"          -> false,
      "range"                 -> true,
      "mapKey"                -> false,
      "mapValue"              -> false,
      "mapTermKey"            -> false,
      "mapTermValue"          -> false,
      "isLink"                -> false,
      "mandatory"             -> false,
      "pattern"               -> false,
      "sorted"                -> false,
      "minimum"               -> false,
      "maximum"               -> false,
      "allowMultiple"         -> false,
      "enum"                  -> false,
      "typeDiscriminatorName" -> false,
      "typeDiscriminator"     -> false,
      "unique"                -> false,
      "patch"                 -> false
  )

  val annotationMapping: Map[String, Boolean] = Map(
    "target"                -> true,
    "propertyTerm"          -> false,
    "range"                 -> true,
    "mapKey"                -> false,
    "mapValue"              -> false,
    "mapTermKey"            -> false,
    "mapTermValue"          -> false,
    "isLink"                -> false,
    "mandatory"             -> false,
    "pattern"               -> false,
    "sorted"                -> false,
    "minimum"               -> false,
    "maximum"               -> false,
    "allowMultiple"         -> false,
    "enum"                  -> false,
    "typeDiscriminatorName" -> false,
    "typeDiscriminator"     -> false,
    "unique"                -> false,
    "patch"                 -> false
  )

  val documentsMapping: Map[String, Boolean] = Map(
      "root"      -> false,
      "fragments" -> false,
      "library"   -> false,
      "options"   -> false
  )

  val documentsMappingOptions: Map[String, Boolean] = Map(
      "selfEncoded"      -> false,
      "declarationsPath" -> false,
      "keyProperty"      -> false,
      "referenceStyle"   -> false
  )

  def closedNode(nodeType: String, id: String, map: YMap)(implicit errorHandler: IllegalTypeHandler): Unit = {
    val allowedProps = nodeType match {
      case "dialect"                 => dialect
      case "library"                 => library
      case "fragment"                => fragment
      case "nodeMapping"             => nodeMapping
      case "propertyMapping"         => propertyMapping
      case "annotationMapping"       => annotationMapping
      case "documentsMapping"        => documentsMapping
      case "documentsMappingOptions" => documentsMappingOptions
    }
    map.map.keySet.map(_.as[YScalar].text).foreach { property =>
      if (!isAnnotation(property)) {
        allowedProps.get(property) match {
          case Some(_) => // correct
          case None    => closedNodeViolation(id, property, nodeType, map)
        }
      }
    }

    allowedProps.foreach {
      case (propName, mandatory) =>
        val props = map.map.keySet.map(_.as[YScalar].text)
        if (mandatory) {
          if (!props.contains(propName)) {
            missingPropertyViolation(id, propName, nodeType, map)
          }
        }
    }
  }

  private def isAnnotation(property: String): Boolean =
    (property.startsWith("(") && property.endsWith(")")) || property.startsWith("x-")

  def link(node: YNode)(implicit errorHandler: IllegalTypeHandler): Either[String, YNode] = {
    node match {
      case _ if isInclude(node) => Left(node.as[YScalar].text)
      case _                    => Right(node)
    }
  }

  private def isInclude(node: YNode) = node.tagType == YType.Include
}
