package amf.rdf.internal

import amf.core.client.scala.AMFGraphConfiguration
import amf.core.client.scala.model.document._
import amf.core.client.scala.model.domain._
import amf.core.internal.metamodel.document.BaseUnitModel
import amf.core.internal.parser.{ParseConfig, ParseConfiguration}
import amf.core.internal.validation.CoreValidations.UnableToParseRdfDocument
import amf.rdf.client.scala.{Node, PropertyObject, RdfModel}
import amf.rdf.internal.graph.NodeFinder
import amf.rdf.internal.parsers._

object RdfModelParser {
  def apply(config: ParseConfiguration, entities: EntitiesFacade): RdfModelParser =
    new RdfModelParser(config, entities)
  def apply(amfConfig: AMFGraphConfiguration): RdfModelParser = {
    val config = ParseConfig(amfConfig)
    apply(config, new EntitiesFacade(config))
  }
}

class RdfModelParser(config: ParseConfiguration, facade: EntitiesFacade) extends RdfParserCommon {

  override implicit val ctx: RdfParserContext = new RdfParserContext(config = config)

  def parse(model: RdfModel, location: String): BaseUnit = {
    val unit = model.findNode(location) match {
      case Some(rootNode) =>
        // assumes root is always an Obj
        val nodeFinder = new NodeFinder(model)
        val parser =
          new ObjectParser(location, new RecursionControl(), facade, nodeFinder, new SourcesRetriever(nodeFinder))
        parser.parse(rootNode, findBaseUnit = true) match {
          case Some(unit: BaseUnit) =>
            unit.set(BaseUnitModel.Location, location.split("#").head)
            unit
          case _ =>
            ctx.eh.violation(UnableToParseRdfDocument,
                             location,
                             s"Unable to parse RDF model for location root node: $location")
            Document()
        }
      case _ =>
        ctx.eh.violation(UnableToParseRdfDocument,
                         location,
                         s"Unable to parse RDF model for location root node: $location")
        Document()
    }

    // Resolve annotations after parsing entire graph
    ctx.collected.collect({ case r: ResolvableAnnotation => r }) foreach (_.resolve(ctx.nodes))
    unit
  }
}

class RecursionControl(private var visited: Set[String] = Set()) {
  def visited(node: Node): Unit = {
    this.visited = visited + node.subject
  }
  def hasVisited(node: Node): Boolean               = visited.contains(node.subject)
  def hasVisited(property: PropertyObject): Boolean = visited.contains(property.value)
}
