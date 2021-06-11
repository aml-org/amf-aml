package amf.aml.internal.transform.domain

import amf.aml.internal.metamodel.domain.NodeMappingModel
import amf.aml.client.scala.model.domain.{NodeMapping, PropertyMapping}

import scala.collection.mutable

class NodeMappingResolver(val child: NodeMapping) {

  def resolveExtension: NodeMapping = {
    child.extend.foreach { case parent: NodeMapping =>
      val resolvedParent = new NodeMappingResolver(parent).resolveExtension

      resolveIdTemplate(child, resolvedParent)
      resolvePropertyMappings(child, resolvedParent)

      // we store the extended reference and remove the extends property
      child.withResolvedExtends(child.resolvedExtends ++ Seq(parent.id))
    }
    child.fields.removeField(NodeMappingModel.Extends)
    child
  }

  private def resolveIdTemplate(child: NodeMapping, resolvedParent: NodeMapping): Unit = {
    resolvedParent.idTemplate.option() match {
      case Some(idTemplate) =>
        child.idTemplate.option() match {
          case None => child.withIdTemplate(idTemplate)
          case _    => // ignore
        }
      case _ => // ignore
    }
  }

  def resolvePropertyMappings(child: NodeMapping, parent: NodeMapping): Unit = {
    val childPropertyMappings = child
      .propertiesMapping()
      .toStream
      .map(_.name().value())
      .toSet

    val definedInChild = (property: PropertyMapping) => childPropertyMappings.contains(property.name().value())

    val inheritedProperties = parent
      .propertiesMapping()
      .toStream
      .filter(property => !definedInChild(property))
      .map { inheritedProperty =>
        PropertyMapping(inheritedProperty.fields.copy(), inheritedProperty.annotations.copy())
          .withId(inheritedProperty.id)
      }

    child.withPropertiesMapping(child.propertiesMapping() ++ inheritedProperties)
  }

}
