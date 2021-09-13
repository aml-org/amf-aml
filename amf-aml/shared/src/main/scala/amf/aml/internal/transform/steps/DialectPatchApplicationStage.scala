package amf.aml.internal.transform.steps

import amf.core.internal.annotations.LexicalInformation
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.internal.metamodel.Field
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfElement, AmfScalar}
import amf.core.internal.parser.domain.Value
import amf.aml.internal.metamodel.document.DialectInstanceModel
import amf.aml.internal.metamodel.domain.MergePolicies._
import amf.aml.client.scala.model.document.{DialectInstance, DialectInstancePatch}
import amf.aml.client.scala.model.domain._
import amf.aml.internal.validate.DialectValidations.InvalidDialectPatch
import amf.core.client.scala.transform.TransformationStep

import scala.language.postfixOps

class DialectPatchApplicationStage() extends TransformationStep {
  override def transform(model: BaseUnit, errorHandler: AMFErrorHandler): BaseUnit = {
    new DialectPatchApplication()(errorHandler).transform(model)
  }
}

private class DialectPatchApplication()(implicit val errorHandler: AMFErrorHandler) {
  // Define types for better readability
  type TargetDomainElement = DialectDomainElement // Domain element from target
  type PatchDomainElement  = DialectDomainElement // Domain element from patch
  type MergedDomainElement = DialectDomainElement // Domain element resulting from target-patch merge
  type NeutralId           = String               // ID relative to a location

  def transform[T <: BaseUnit](model: T): T = {
    model match {
      case patch: DialectInstancePatch => resolvePatch(patch).asInstanceOf[T]
      case _                           => model
    }
  }

  private def resolvePatch(patch: DialectInstancePatch): BaseUnit = {
    findTarget(patch) match {
      case Some(target: DialectInstance) =>
        applyPatch(target, patch)
      case _ =>
        patch
    }
  }

  private def findTarget(patch: DialectInstancePatch): Option[DialectInstance] = {
    patch.extendsModel.option() match {
      case Some(id) if patch.location().isDefined =>
        patch.references.find(u => u.isInstanceOf[DialectInstance] && u.location().get.endsWith(id)) match {
          case Some(d: DialectInstance) if d.location().isDefined => Some(d)
          case _                                                  => None
        }
      case _ => None
    }
  }

  private def applyPatch(target: DialectInstance, patch: DialectInstancePatch): DialectInstance = {
    patchNode(Some(target.encodes.asInstanceOf[DialectDomainElement]),
              target.location().get,
              patch.encodes.asInstanceOf[DialectDomainElement],
              patch.location().get) match {
      case Some(patchedDialectElement) => target.withEncodes(patchedDialectElement)
      case None =>
        target.fields.remove(DialectInstanceModel.Encodes.value.iri())
        target
    }
  }

  private def patchNode(targetNode: Option[DialectDomainElement],
                        targetLocation: String,
                        patchNode: DialectDomainElement,
                        patchLocation: String): Option[DialectDomainElement] = {
    findNodeMergePolicy(patchNode) match {
      case INSERT =>
        patchNodeInsert(targetNode, targetLocation, patchNode, patchLocation)
      case DELETE =>
        patchNodeDelete(targetNode, targetLocation, patchNode, patchLocation)
      case UPDATE =>
        patchNodeUpdate(targetNode, targetLocation, patchNode, patchLocation)
      case UPSERT =>
        patchNodeUpsert(targetNode, targetLocation, patchNode, patchLocation)
      case IGNORE =>
        targetNode
      case FAIL =>
        errorHandler.violation(
            InvalidDialectPatch,
            patchNode.id,
            None,
            s"Node ${patchNode.meta.`type`.map(_.iri()).mkString(",")} cannot be patched",
            patchNode.annotations.find(classOf[LexicalInformation]),
            None
        )
        None
    }
  }

  private def findNodeMergePolicy(element: DialectDomainElement): String =
    element.definedBy.mergePolicy.option().getOrElse("update")
  private def findPropertyMappingMergePolicy(property: PropertyMapping): String =
    property.mergePolicy.option().getOrElse("update")

  // add or ignore if present
  private def patchNodeInsert(targetNode: Option[DialectDomainElement],
                              targetLocation: String,
                              patchNode: DialectDomainElement,
                              patchLocation: String): Option[DialectDomainElement] = {
    if (targetNode.isEmpty) Some(patchNode) else targetNode
  }

  // delete or ignore if not present
  private def patchNodeDelete(targetNode: Option[DialectDomainElement],
                              targetLocation: String,
                              patchNode: DialectDomainElement,
                              patchLocation: String): Option[DialectDomainElement] = {
    if (targetNode.nonEmpty && sameNodeIdentity(targetNode.get, targetLocation, patchNode, patchLocation)) {
      None
    } else {
      targetNode
    }
  }

  private def patchProperty(targetNode: DialectDomainElement,
                            patchField: Field,
                            patchValue: Value,
                            propertyMapping: PropertyMapping,
                            targetLocation: String,
                            patchLocation: String): Unit = {
    propertyMapping.classification() match {
      case LiteralProperty =>
        patchLiteralProperty(targetNode, patchField, patchValue, propertyMapping, targetLocation, patchLocation)
      case LiteralPropertyCollection =>
        patchLiteralCollectionProperty(targetNode, patchField, patchValue, propertyMapping)
      case ObjectProperty =>
        patchObjectProperty(targetNode, patchField, patchValue, propertyMapping, targetLocation, patchLocation)
      case ObjectPropertyCollection | ObjectMapProperty | ObjectPairProperty =>
        patchObjectCollectionProperty(targetNode,
                                      patchField,
                                      patchValue,
                                      propertyMapping,
                                      targetLocation,
                                      patchLocation)
      case _ =>
      // throw new Exception("Unsupported node mapping in patch")

    }
  }

  private def patchLiteralProperty(targetNode: DialectDomainElement,
                                   patchField: Field,
                                   patchValue: Value,
                                   propertyMapping: PropertyMapping,
                                   targetLocation: String,
                                   patchLocation: String): Unit = {
    findPropertyMappingMergePolicy(propertyMapping) match {
      case INSERT if !targetNode.graph.containsField(patchField) =>
        targetNode.graph.patchField(patchField, patchValue)
      case DELETE if targetNode.graph.containsField(patchField) =>
        try {
          if (targetNode.fields.getValue(patchField).value.asInstanceOf[AmfScalar].value
                == patchValue.value.asInstanceOf[AmfScalar].value)
            targetNode.graph.removeField(patchField.toString)
        } catch {
          case _: Exception => // ignore
        }
      case UPDATE if targetNode.graph.containsField(patchField) =>
        targetNode.graph.patchField(patchField, patchValue)
      case UPSERT =>
        targetNode.graph.patchField(patchField, patchValue)
      case IGNORE =>
      // ignore
      case FAIL =>
        errorHandler.violation(
            InvalidDialectPatch,
            targetNode.id,
            None,
            s"Property ${patchField.value.iri()} cannot be patched",
            targetNode.fields.getValue(patchField).annotations.find(classOf[LexicalInformation]),
            None
        )
      case _ =>
      // ignore
    }
  }

  private def getCollectionFrom(element: DialectDomainElement, field: Field) = {
    element.fields.getValueAsOption(field) match {
      case Some(v) if v.value.isInstanceOf[AmfArray] => v.value.asInstanceOf[AmfArray].values
      case Some(v)                                   => Seq(v.value)
      case _                                         => Nil
    }
  }

  private def getCollectionFrom(value: Value): Seq[AmfElement] = {
    value.value match {
      case arr: AmfArray => arr.values
      case elm           => Seq(elm)
    }
  }

  private def patchLiteralCollectionProperty(targetNode: DialectDomainElement,
                                             patchField: Field,
                                             patchValue: Value,
                                             propertyMapping: PropertyMapping): Unit = {

    val targetPropertyValue = getCollectionFrom(targetNode, patchField).toSet
    val patchPropertyValue  = getCollectionFrom(patchValue).toSet

    findPropertyMappingMergePolicy(propertyMapping) match {
      case INSERT =>
        targetNode.graph.patchSeqField(patchField, targetPropertyValue.union(patchPropertyValue).toSeq)
      case DELETE =>
        targetNode.graph.patchSeqField(patchField, targetPropertyValue.diff(patchPropertyValue).toSeq)
      case UPDATE =>
        targetNode.graph.patchSeqField(patchField, patchPropertyValue.toSeq)
      case UPSERT =>
        targetNode.graph.patchSeqField(patchField, targetPropertyValue.union(patchPropertyValue).toSeq)
      case IGNORE =>
      // ignore
      case FAIL =>
        errorHandler.violation(
            InvalidDialectPatch,
            targetNode.id,
            None,
            s"Property ${patchField.value.iri()} cannot be patched",
            patchValue.annotations.find(classOf[LexicalInformation]),
            None
        )
      case _ =>
      // ignore
    }
  }

  private def neutralId(id: String, location: String): String = {
    id.replace(location, "")
  }

  private def onlyDomainElements(elements: Seq[AmfElement]): Seq[DialectDomainElement] = {
    val dialectDomainElements   = (element: AmfElement) => element.isInstanceOf[DialectDomainElement]
    val asDialectDomainElements = (element: AmfElement) => element.asInstanceOf[DialectDomainElement]
    elements
      .filter(dialectDomainElements)
      .map(asDialectDomainElements)
  }

  private def idIndex(elements: Seq[DialectDomainElement],
                      targetLocation: String): Map[NeutralId, DialectDomainElement] = {
    val indexById = (element: DialectDomainElement) => neutralId(element.id, targetLocation) -> element
    elements
      .map(indexById)
      .toMap
  }

  private def patchObjectCollectionProperty(targetNode: DialectDomainElement,
                                            patchField: Field,
                                            patchValue: Value,
                                            propertyMapping: PropertyMapping,
                                            targetLocation: String,
                                            patchLocation: String): Unit = {

    val targetElements: Seq[TargetDomainElement] = onlyDomainElements { getCollectionFrom(targetNode, patchField) }
    val patchElements: Seq[PatchDomainElement]   = onlyDomainElements { getCollectionFrom(patchValue) }

    val targetElementsIndex: Map[NeutralId, TargetDomainElement] = idIndex(targetElements, targetLocation)
    val patchElementsIndex: Map[NeutralId, PatchDomainElement]   = idIndex(patchElements, patchLocation)

    val mergePolicy = findPropertyMappingMergePolicy(propertyMapping)
    mergePolicy match {
      case INSERT =>
        // Elements not defined in target
        val toInsertElements = patchElementsIndex
          .filter {
            case (id, _) => !targetElementsIndex.contains(id)
          }
          .values
          .toSeq

        val unionElements = targetElements union toInsertElements

        targetNode.graph.patchSeqField(patchField, unionElements)

      case DELETE =>
        // Elements defined by both patch and target
        val toDeleteElements = patchElementsIndex.flatMap {
          case (id, _) => targetElementsIndex.get(id)
        }.toSeq

        val unionElements = targetElements diff toDeleteElements

        targetNode.graph.patchSeqField(patchField, unionElements)

      case UPDATE | UPSERT =>
        // Merge nodes defined for target and patch
        val mergedElementsIndex: Map[NeutralId, MergedDomainElement] =
          patchElementsIndex.flatMap {
            case (id, patchElement) =>
              for {
                targetElement <- targetElementsIndex.get(id)
                mergedElement <- patchNode(Some(targetElement), targetLocation, patchElement, patchLocation)
              } yield {
                (id, mergedElement)
              }
          }

        val unionElements = mergePolicy match {
          case UPDATE => targetElementsIndex ++ mergedElementsIndex
          case UPSERT => patchElementsIndex ++ targetElementsIndex ++ mergedElementsIndex
        }

        targetNode.graph.patchSeqField(patchField, unionElements.values.toSeq)

      case IGNORE =>
      // ignore

      case FAIL =>
        errorHandler.violation(
            InvalidDialectPatch,
            targetNode.id,
            None,
            s"Property ${patchField.value.iri()} cannot be patched",
            patchValue.annotations.find(classOf[LexicalInformation]),
            None
        )
      case _ =>
      // ignore
    }
  }

  private def patchObjectProperty(targetNode: DialectDomainElement,
                                  patchField: Field,
                                  patchValue: Value,
                                  propertyMapping: PropertyMapping,
                                  targetLocation: String,
                                  patchLocation: String): Unit = {
    patchValue.value match {
      case patchDialectDomainElement: DialectDomainElement =>
        val targetNodeValue = targetNode.fields ? patchField
        patchNode(targetNodeValue, targetLocation, patchDialectDomainElement, patchLocation) match {
          case Some(mergedNode: DialectDomainElement) => targetNode.graph.patchObj(patchField, mergedNode)
          case _                                      => targetNode.graph.removeField(patchField.toString)
        }
      case _ => // ignore
    }
  }

  // recursive merge if both present
  private def patchNodeUpdate(targetNode: Option[DialectDomainElement],
                              targetLocation: String,
                              patchNode: DialectDomainElement,
                              patchLocation: String): Option[DialectDomainElement] = {
    val nodeMapping = patchNode.definedBy
    if (targetNode.isDefined && sameNodeIdentity(targetNode.get, targetLocation, patchNode, patchLocation)) {
      patchNode.meta.fields.foreach { patchField =>
        patchNode.fields.getValueAsOption(patchField) match {
          case Some(fieldValue) =>
            nodeMapping
              .propertiesMapping()
              .find(_.nodePropertyMapping().value() == patchField.value.iri()) match {
              case Some(propertyMapping) =>
                patchProperty(targetNode.get, patchField, fieldValue, propertyMapping, targetLocation, patchLocation)
              case _ => // ignore
            }
          case _ => // ignore
        }
      }
    }
    targetNode
  }

  // recursive merge if both present
  private def patchNodeUpsert(targetNode: Option[DialectDomainElement],
                              targetLocation: String,
                              patchNode: DialectDomainElement,
                              patchLocation: String): Option[DialectDomainElement] = {
    if (targetNode.isEmpty)
      patchNodeInsert(targetNode, targetLocation, patchNode, patchLocation)
    else
      patchNodeUpdate(targetNode, targetLocation, patchNode, patchLocation)
  }

  private def sameNodeIdentity(target: DialectDomainElement,
                               targetLocation: String,
                               patchNode: DialectDomainElement,
                               patchLocation: String): Boolean = {
    neutralId(target.id, targetLocation) == neutralId(patchNode.id, patchLocation)
  }

}
