package amf.plugins.document.vocabularies.resolution.domain

import amf.plugins.document.vocabularies.metamodel.domain.NodeMappingModel
import amf.plugins.document.vocabularies.model.domain.{NodeMapping, PropertyMapping}

import scala.collection.mutable

class NodeMappingResolver(val nodeMapping:NodeMapping) {

  def resolveExtension: NodeMapping = {
    nodeMapping.extend match {
      case (parent: NodeMapping) :: _ =>
        val superMerged = new NodeMappingResolver(parent).resolveExtension
        superMerged.idTemplate.option() match {
          case Some(idTemplate) =>
            nodeMapping.idTemplate.option() match {
              case None => nodeMapping.withIdTemplate(idTemplate)
              case _    => // ignore
            }
          case _ => // ignore
        }

        val merged = mergeWith(superMerged)
        // we store the extended reference and remove the extends property
        merged.withResolvedExtends(Seq(parent.id))
        merged.fields.removeField(NodeMappingModel.Extends)
        // return the final node
        merged
      case _ =>
      // Ignore
    }
    nodeMapping
  }

  def mergeWith(other: NodeMapping): NodeMapping = {
    val acc = mutable.Map[String, PropertyMapping]()
    nodeMapping.propertiesMapping().foreach { prop =>
      acc += (prop.name().value() -> prop)
    }

    other.propertiesMapping().foreach { property =>
      acc.get(property.name().value()) match {
        case Some(_) => // Ignore
        case None =>
          acc += (property.name().value() -> PropertyMapping(property.fields.copy(), property.annotations.copy())
            .withId(property.id))
      }
    }

    nodeMapping.withPropertiesMapping(acc.values.toList)
  }

}
