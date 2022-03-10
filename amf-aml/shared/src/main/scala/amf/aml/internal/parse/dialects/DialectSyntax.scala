package amf.aml.internal.parse.dialects

import org.yaml.model.{IllegalTypeHandler, YMap, YNode, YScalar, YType}

trait DialectSyntax { this: DialectContext =>
  type Required = Boolean

  val dialect: Map[String, Required] = Map(
      "$id"                -> false,
      "$type"              -> false,
      "dialect"            -> true,
      "version"            -> true,
      "usage"              -> false,
      "external"           -> false,
      "uses"               -> false,
      "nodeMappings"       -> false,
      "documents"          -> false,
      "annotationMappings" -> false,
      "extensions"         -> false
  )

  val library: Map[String, Required] = Map(
      "$id"          -> false,
      "$type"        -> false,
      "usage"        -> false,
      "external"     -> false,
      "uses"         -> false,
      "nodeMappings" -> false
  )

  val nodeMapping: Map[String, Required] = Map(
      "classTerm"            -> false,
      "mapping"              -> false,
      "idProperty"           -> false,
      "idTemplate"           -> false,
      "patch"                -> false,
      "extends"              -> false,
      "union"                -> false,
      "conditional"          -> false,
      "additionalProperties" -> false
  )

  val conditionalMapping: Map[String, Required] = Map(
      "if"   -> true,
      "then" -> true,
      "else" -> true,
  )

  val propertyLikeMapping: Map[String, Required] = Map(
      "range"                 -> true,
      "propertyTerm"          -> false,
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
      "unique"                -> false
  )

  val annotationMapping: Map[String, Required] = propertyLikeMapping ++ Map(
      "domain" -> false
  )

  val fragment: Map[String, Required] = Map(
      "$id"      -> false,
      "usage"    -> false,
      "external" -> false,
      "uses"     -> false
  ) ++ nodeMapping

  val propertyMapping: Map[String, Required] = propertyLikeMapping ++ Map(
      "mapKey"       -> false,
      "mapValue"     -> false,
      "mapTermKey"   -> false,
      "mapTermValue" -> false,
      "patch"        -> false,
      "default"      -> false
  )

  val documentsMapping: Map[String, Required] = Map(
      "root"      -> false,
      "fragments" -> false,
      "library"   -> false,
      "options"   -> false
  )

  val documentsMappingOptions: Map[String, Required] = Map(
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
      case "conditionalMapping"      => conditionalMapping
      case "annotationMapping"       => annotationMapping
      case "propertyMapping"         => propertyMapping
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
