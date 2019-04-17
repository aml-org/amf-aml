package amf.plugins.document.vocabularies.resolution.stages

import amf.core.model.document.{BaseUnit, DeclaresModel}
import amf.core.parser.ErrorHandler
import amf.core.resolution.stages.ResolutionStage
import amf.plugins.document.vocabularies.metamodel.domain.NodeMappingModel
import amf.plugins.document.vocabularies.model.domain.{NodeMapping, PropertyMapping}

import scala.collection.mutable

class DialectNodeExtensionStage()(override implicit val errorHandler: ErrorHandler) extends ResolutionStage() {

  override def resolve[T <: BaseUnit](model: T): T = {
    model match {
      case declarationModel: DeclaresModel =>
        declarationModel.declares.foreach {
          case nodeMapping: NodeMapping =>
            mergeNode(nodeMapping)
          case _ => // ignore
        }
      case _ => // ignore
    }
    model
  }

  def mergeNode(nodeMapping: NodeMapping): NodeMapping = {
    nodeMapping.extend match {
      case (superNodeMapping: NodeMapping) :: _ =>
        val superMerged = mergeNode(superNodeMapping)
        superMerged.idTemplate.option() match {
          case Some(idTemplate) =>
            nodeMapping.idTemplate.option() match {
              case None => nodeMapping.withIdTemplate(idTemplate)
              case _    => // ignore
            }
          case _ => // ignore
        }
        mergeNodeMapping(nodeMapping, superMerged)
      case _                     =>
        nodeMapping
    }
    nodeMapping.fields.removeField(NodeMappingModel.Extends)
    nodeMapping
  }

  def mergeNodeMapping(nodeMapping: NodeMapping, superMerged: NodeMapping): NodeMapping = {
    val acc = mutable.Map[String, PropertyMapping]()
    nodeMapping.propertiesMapping().foreach {  prop =>
      acc += (prop.name().value() -> prop)
    }

    superMerged.propertiesMapping().foreach { property =>
      acc.get(property.name().value()) match {
        case Some(existingProperty) => // ignore
        case None                   => acc += (property.name().value() -> PropertyMapping(property.fields.copy(), property.annotations.copy()).withId(property.id))
      }
    }


    nodeMapping.withPropertiesMapping(acc.values.toList)
  }
}
