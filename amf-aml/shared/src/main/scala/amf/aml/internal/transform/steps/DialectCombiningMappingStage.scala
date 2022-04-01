package amf.aml.internal.transform.steps

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.NodeMappable.AnyNodeMappable
import amf.aml.client.scala.model.domain.{AnyMapping, NodeMapping, UnionNodeMapping}
import amf.aml.internal.metamodel.domain.{AnyMappingModel, NodeMappingModel}
import amf.aml.internal.render.emitters.common.IdCounter
import amf.aml.internal.render.emitters.instances.{DefaultNodeMappableFinder, DialectIndex}
import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.model.StrField
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.AmfScalar
import amf.core.client.scala.transform.TransformationStep
import amf.core.internal.annotations.VirtualElement
import amf.core.internal.parser.domain.Annotations

import scala.annotation.tailrec
import scala.collection.mutable

// This stage generates all the combining mappings of an allOf field.
// This is needed to have an indexed list of the mappings to link from a parsed DialectDomainElement
// Currently supported allOf, allOf with properties, oneOf with properties and conditional with properties.
// Not supported allOf, oneOf and conditional mixed between them.
class DialectCombiningMappingStage extends TransformationStep() {

  var dialect: Option[Dialect]                 = None
  var index: Option[DialectIndex]              = None
  var visitedCombinations: mutable.Set[String] = mutable.Set()
  val counter: IdCounter                       = new IdCounter

  override def transform(model: BaseUnit,
                         errorHandler: AMFErrorHandler,
                         configuration: AMFGraphConfiguration): BaseUnit = {

    model match {
      case dialect: Dialect =>
        this.index = Some(DialectIndex(dialect, new DefaultNodeMappableFinder(Seq(dialect))))
        this.dialect = Some(dialect)
        dialect.declares.foreach {
          case anyMapping: AnyMapping if isCombinationGroup(anyMapping) && !alreadyProcessed(anyMapping) =>
            processCombinationGroup(getCombinationGroup(anyMapping))
          case _ => // ignore
        }
      case _ => // ignore
    }

    model
  }

  private def processCombinationGroup(combinators: Seq[AnyNodeMappable]): Unit = {
    val combinationsRaw = combinators.map(processCombinationElement).toList
    val combinationsIDs = generateCombinations(combinationsRaw)
    combinationsIDs.foreach(generateCombinationMapping)
  }

  private def processCombinationElement(element: AnyNodeMappable): List[String] = element match {
    // If the element is an and it should be processed in a different way
    // The and should be evaluated as an all, so we will return his ID and not the one of the components
    case combination: AnyMapping if isCombinationGroup(combination) =>
      if (!alreadyProcessed(combination)) processCombinationGroup(getCombinationGroup(combination))
      List(combination.id)
    case otherMapping: AnyMapping =>
      collectComponents(otherMapping) match {
        // The cut condition is that the leaf is a NodeMapping
        case uniqueComponent :: Nil if uniqueComponent.isInstanceOf[NodeMapping] =>
          List(uniqueComponent.id)
        // I can flatmap here, the only cases where is not an or of all the element is the and
        case otherComponents =>
          otherComponents.flatMap(processCombinationElement).toList
      }
  }

  // This method combines the different mappings exclusions
  private def generateCombinations(combinationsRaw: List[List[String]]): List[List[String]] =
    cartesianProduct(combinationsRaw)

  // This method generates a mapping based on the combination components
  // It also register the mapping as a declaration in the Dialect
  private def generateCombinationMapping(components: Seq[String]): Unit = {
    val mapping = NodeMapping(Annotations(VirtualElement()))
    setMappingName(mapping)
    registerMappingDeclaration(mapping)
    mapping.setArrayWithoutId(AnyMappingModel.Components, components.map(c => AmfScalar(c)))
    val componentMappings =
      components.map(findMapping).map(component => component.link(component.name.toString).asInstanceOf[NodeMapping])
    mapping.withExtends(componentMappings)
    mapping.extend.zipWithIndex.foreach {
      case (e, i) => e.withId(s"${e.id}-link-extends-$i")
    }
    // TODO: We will set here an 'additionalProperties: true' for all combination mappings. Maybe this should be inherit from the extends in a future
    mapping.withClosed(false)
  }

  // This method collect the schema defined at the same level
  private def collectComponents(mapping: AnyMapping): Seq[AnyNodeMappable] = mapping match {
    // The and is an special case, it will be processed in the next cycle
    case combination: AnyMapping if isCombinationGroup(combination) => Seq(combination.asInstanceOf[AnyNodeMappable])
    case other: AnyNodeMappable                                     => getComponents(other)
  }

  private def getComponents(mapping: AnyMapping): Seq[AnyNodeMappable] = mapping match {
    case and: AnyMapping if and.isAny => findAllMappings(and.and)
    case or: AnyMapping if or.isOr    => findAllMappings(or.or)
    case conditional: AnyMapping if conditional.isConditional =>
      findAllMappings(Seq(conditional.thenMapping, conditional.elseMapping))
    case union: UnionNodeMapping => findAllMappings(union.objectRange())
    case other: AnyNodeMappable  => Seq(other)
  }

  // Evaluates if the mapping is a combination group (an allOf or an oneOf/conditional with extended mapping)
  private def isCombinationGroup(anyMapping: AnyMapping) = anyMapping match {
    case any: AnyMapping if any.isAny                                                 => true
    case node: NodeMapping if node.hasProperties && (node.isOr || node.isConditional) => true
    case _                                                                            => false
  }

  // This method returns the components + the extended schema
  // It assumes that you already evaluated that it is an combination group
  private def getCombinationGroup(anyMapping: AnyMapping): Seq[AnyNodeMappable] = {
    val extendedMapping = getExtendedMapping(anyMapping)
    anyMapping match {
      case any if any.isAny   => getComponents(anyMapping) ++ extendedMapping
      case other: NodeMapping =>
        // In this flow I already know that there is an extended mapping, because I know that it is an combination group and it is not an any
        // Here I will generate an oneOf/conditional + props in a allOf like structure
        // (I will return the oneOf/conditional in one mapping and the properties in other, like they are elements of a parent allOf)
        generateAllOfLikeElements(other)
    }
  }

  // If there is an extended mapping I will return the same mapping.
  // It is already collected so it will be ignored in the next cycle.
  private def getExtendedMapping(anyMapping: AnyMapping): Option[NodeMapping] = anyMapping match {
    case nodeMapping: NodeMapping if nodeMapping.propertiesMapping().nonEmpty => Some(nodeMapping)
    case _                                                                    => None
  }

  // This method generates a mapping copy removing certain fields.
  // This is necessary because a extended oneOf/conditional is basically 2 different mappings.
  // So I need to generate a combination with both of them
  private def generateAllOfLikeElements(baseMapping: NodeMapping): Seq[NodeMapping] = {
    val temporalBase = generateClone(baseMapping)
    removeNodeMappingFields(temporalBase)
    val temporalExtension = generateClone(baseMapping)
    removeCombiningFields(temporalExtension)
    Seq(temporalBase, temporalExtension)
  }

  private def generateClone(original: NodeMapping): NodeMapping = {
    val copy = original.copyElement().asInstanceOf[NodeMapping]
    copy.withName(original.name.value())
    copy.withId(original.id)
    copy
  }

  private def removeCombiningFields(mapping: NodeMapping): Unit = {
    mapping.fields.removeField(AnyMappingModel.And)
    mapping.fields.removeField(AnyMappingModel.Or)
    mapping.fields.removeField(AnyMappingModel.If)
    mapping.fields.removeField(AnyMappingModel.Then)
    mapping.fields.removeField(AnyMappingModel.Else)
  }

  private def removeNodeMappingFields(mapping: NodeMapping): Unit = {
    mapping.fields.removeField(NodeMappingModel.PropertiesMapping)
  }

  private def findAllMappings(mappingIds: Seq[StrField]): Seq[AnyNodeMappable] = mappingIds.map(findMapping)

  private def findMapping(mapping: StrField): AnyNodeMappable = findMapping(mapping.toString)

  private def findMapping(mapping: String): AnyNodeMappable = index.get.findNodeMappingById(mapping)._2

  private def setMappingName(mapping: NodeMapping) = mapping.withName(newMappingName)

  private def newMappingName: String = counter.genId(s"CombiningMapping")

  private def registerMappingDeclaration(mapping: NodeMapping): Unit = dialect.get.withDeclaredElement(mapping)

  private def alreadyProcessed(anyMapping: AnyMapping) =
    if (visitedCombinations.contains(anyMapping.id)) true
    else {
      visitedCombinations += anyMapping.id
      false
    }

  // TODO extract this to scala-common
  private def cartesianProduct[T](lst: List[List[T]]): List[List[T]] = {

    @tailrec
    def pel(e: T, ll: List[List[T]], a: List[List[T]] = Nil): List[List[T]] =
      ll match {
        case Nil     => a.reverse
        case x :: xs => pel(e, xs, (e :: x) :: a)
      }

    lst match {
      case Nil      => Nil
      case x :: Nil => List(x)
      case x :: _ =>
        x match {
          case Nil => Nil
          case _ =>
            lst.foldRight(List(x))((l, a) => l.flatMap(pel(_, a))).map(_.dropRight(x.size))
        }
    }
  }

}
