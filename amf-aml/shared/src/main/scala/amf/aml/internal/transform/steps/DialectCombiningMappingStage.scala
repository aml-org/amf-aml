package amf.aml.internal.transform.steps

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.NodeMappable.AnyNodeMappable
import amf.aml.client.scala.model.domain.{AnyMapping, ConditionalNodeMapping, NodeMapping, UnionNodeMapping}
import amf.aml.internal.metamodel.domain.AnyMappingModel
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
          case anyMapping: AnyMapping if anyMapping.and.nonEmpty && !alreadyProcessed(anyMapping) =>
            processCombinationGroup(findAllMappings(anyMapping.and))
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
    case combination: AnyMapping if combination.and.nonEmpty =>
      if (!alreadyProcessed(combination)) processCombinationGroup(collectComponents(combination))
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
    mapping.withName(newMappingName)
    dialect.get.withDeclaredElement(mapping)
    mapping.setArrayWithoutId(AnyMappingModel.Components, components.map(c => AmfScalar(c)))
    val componentMappings =
      components.map(findMapping).map(component => component.link(component.name.toString).asInstanceOf[NodeMapping])
    mapping.withExtends(componentMappings)
    mapping.extend.zipWithIndex.foreach {
      case (e, i) => e.withId(s"${e.id}-link-extends-${i}")
    }
  }

  // This method collect the schema defined at the same level
  // TODO currently the extension schemas will be resolved in SemJSONSchema transformation, but in a future should be resolved here
  private def collectComponents(mapping: AnyMapping): Seq[AnyNodeMappable] = mapping match {
    // The and is an special case, it will be processed in the next cycle
    case and: AnyMapping if and.and.nonEmpty =>
      Seq(and.asInstanceOf[AnyNodeMappable])
    case or: AnyMapping if or.or.nonEmpty =>
      findAllMappings(or.or)
    case union: UnionNodeMapping =>
      findAllMappings(union.objectRange())
    case conditional: ConditionalNodeMapping =>
      findAllMappings(Seq(conditional.thenMapping, conditional.elseMapping))
    case other: AnyNodeMappable =>
      Seq(other)
  }

  private def findAllMappings(mappingIds: Seq[StrField]): Seq[AnyNodeMappable] = mappingIds.map(findMapping)

  private def findMapping(mapping: StrField): AnyNodeMappable = findMapping(mapping.toString)

  private def findMapping(mapping: String): AnyNodeMappable = index.get.findNodeMappingById(mapping)._2

  private def newMappingName: String = counter.genId(s"CombiningMapping")

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
