package amf.aml.internal.parse.instances

import amf.aml.client.scala.model.document._
import amf.aml.client.scala.model.domain._
import amf.aml.internal.annotations.FromUnionNodeMapping
import amf.aml.internal.metamodel.document.DialectInstanceModel
import amf.aml.internal.metamodel.domain.DialectDomainElementModel
import amf.aml.internal.parse.common.{DeclarationKey, DeclarationKeyCollector}
import amf.aml.internal.parse.instances.DialectInstanceParser._
import amf.aml.internal.parse.instances.parser._
import amf.aml.internal.validate.DialectValidations.DialectError
import amf.core.client.scala.model.document.EncodesModel
import amf.core.client.scala.model.domain.AmfScalar
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.parser.{Root, YMapOps, YNodeLikeOps}
import amf.core.internal.utils._
import com.github.ghik.silencer.silent
import org.yaml.model._

// TODO: needs further breakup of parts. This components of this class are untestable the current way.
// TODO: find out why all these methods are protected.
// TODO:

object DialectInstanceParser extends NodeMappableHelper {
  def typesFrom(mapping: NodeMapping): Seq[String] = {
    Seq(mapping.nodetypeMapping.option(), Some(mapping.id)).flatten
  }

  def encodedElementDefaultId(dialectInstance: EncodesModel)(implicit ctx: DialectInstanceContext): String =
    if (Option(ctx.dialect.documents()).flatMap(_.selfEncoded().option()).getOrElse(false))
      dialectInstance.location().getOrElse(dialectInstance.id)
    else
      dialectInstance.id + "#/encodes"

  @scala.annotation.tailrec
  def findDeclarationsMap(paths: List[String], map: YMap)(implicit ctx: DialectInstanceContext): Option[YMap] = {
    paths match {
      case Nil => Some(map)
      case head :: tail =>
        map.key(head) match {
          case Some(m) if m.value.tagType == YType.Map =>
            if (tail.nonEmpty) findDeclarationsMap(tail, m.value.as[YMap])
            else m.value.toOption[YMap]
          case Some(o) =>
            ctx.eh
              .violation(
                DialectError,
                "",
                s"Invalid node type for declarations path ${o.value.tagType.toString()}",
                o.location
              )
            None
          case _ => None
        }
    }
  }

  def emptyElement(defaultId: String, ast: YNode, mappable: NodeMappable, givenAnnotations: Option[Annotations])(
      implicit ctx: DialectInstanceContext
  ): DialectDomainElement = {
    val mappings = mappable match {
      case m: NodeMapping => Seq(m.nodetypeMapping.value(), mappable.id)
      case _              => Seq(mappable.id)
    }
    lazy val ann = mappable match {
      case u: UnionNodeMapping => Annotations(ast) += FromUnionNodeMapping(u)
      case _                   => Annotations(ast)
    }
    val element = DialectDomainElement(givenAnnotations.getOrElse(ann))
      .withId(defaultId)
      .withInstanceTypes(mappings)
    ctx.eh.warning(DialectError, defaultId, s"Empty map: ${mappings.head}", ast.location)
    element
  }

  def computeParsingScheme(nodeMap: YMap): String =
    if (nodeMap.key("$include").isDefined)
      "$include"
    else if (nodeMap.key("$ref").isDefined)
      "$ref"
    else
      "inline"

  def pathSegment(parent: String, nexts: List[String]): String = {
    var path = parent
    nexts.foreach { n =>
      path = if (path.endsWith("/")) {
        path + n.urlComponentEncoded
      } else {
        path + "/" + n.urlComponentEncoded
      }
    }
    path
  }
}

class DialectInstanceParser(val root: Root)(implicit val ctx: DialectInstanceContext)
    extends DeclarationKeyCollector
    with NodeMappableHelper
    with InstanceNodeIdHandling {

  val map: YMap = root.parsed.asInstanceOf[SyamlParsedDocument].document.as[YMap]

  def parseDocument(): DialectInstance = {
    @silent("deprecated") // Silent can only be used in assignment expressions
    val dialectInstance: DialectInstance = DialectInstance(Annotations(map))
      .withLocation(root.location)
      .withId(root.location)
      .withProcessingData(DialectInstanceProcessingData().withTransformed(false).withDefinedBy(ctx.dialect.id))
      .withDefinedBy(ctx.dialect.id)
    parseDeclarations("root")

    val references =
      DialectInstanceReferencesParser(dialectInstance, map, root.references)
        .parse(dialectInstance.location().getOrElse(dialectInstance.id))

    if (ctx.declarations.externals.nonEmpty)
      dialectInstance.withExternals(ctx.declarations.externals.values.toSeq)

    val dialectDomainElement = parseEncoded(dialectInstance)
    // registering JSON pointer
    ctx.registerJsonPointerDeclaration(root.location + "#/", dialectDomainElement)

    dialectInstance.set(DialectInstanceModel.Encodes, dialectDomainElement, Annotations.inferred())
    addDeclarationsToModel(dialectInstance)

    if (references.baseUnitReferences().nonEmpty)
      dialectInstance.withReferences(references.baseUnitReferences())
    if (ctx.nestedDialects.nonEmpty) {
      dialectInstance.processingData.withGraphDependencies(ctx.nestedDialects.map(nd => nd.location().getOrElse(nd.id)))
      @silent("deprecated") // Silent can only be used in assignment expressions
      val a = dialectInstance.withGraphDependencies(ctx.nestedDialects.map(nd => nd.location().getOrElse(nd.id)))
    }

    // resolve unresolved references
    ctx.futureDeclarations.resolve()

    dialectInstance
  }

  protected def parseDeclarations(documentType: String): Unit = {
    val declarationsNodeMappings = if (documentType == "root") {
      ctx.rootDeclarationsNodeMappings
    } else {
      ctx.libraryDeclarationsNodeMappings
    }

    val pathOption = Option(ctx.dialect.documents()).flatMap(d => d.declarationsPath().option())
    val normalizedPath =
      pathOption.map(p => if (p.startsWith("/")) p else "/" + p).map(p => if (p.endsWith("/")) p else p + "/")
    val paths: List[String] = pathOption.map(_.split("/").toList).getOrElse(Nil)
    findDeclarationsMap(paths, map).foreach { declarationsMap =>
      declarationsNodeMappings.foreach { case (name, nodeMapping) =>
        declarationsMap.entries.find(_.key.as[YScalar].text == name).foreach { entry =>
          addDeclarationKey(DeclarationKey(entry))
          val declarationsId = root.location + "#" + normalizedPath.getOrElse("/") + name.urlComponentEncoded
          entry.value.as[YMap].entries.foreach { declarationEntry =>
            val declarationName = declarationEntry.key.as[YScalar].text
            val id              = pathSegment(declarationsId, List(declarationName))
            val node = InstanceElementParser(root).parse(
              declarationsId,
              id,
              declarationEntry.value,
              nodeMapping,
              givenAnnotations = Some(Annotations(declarationEntry))
            )

            // lookup by ref name
            node.set(
              DialectDomainElementModel.DeclarationName,
              AmfScalar(declarationName, Annotations(declarationEntry.key)),
              Annotations(declarationEntry.key)
            )
            ctx.declarations.registerDialectDomainElement(declarationEntry.key, node)
            // lookup by JSON pointer, absolute URI
            ctx.registerJsonPointerDeclaration(id, node)
          }
        }
      }
    }
  }

  protected def parseEncoded(dialectInstance: EncodesModel): DialectDomainElement = {
    val result = for {
      documents <- Option(ctx.dialect.documents())
      mapping   <- Option(documents.root())
    } yield {
      ctx.findNodeMapping(mapping.encoded().value()) match {
        case Some(nodeMapping) =>
          val path = dialectInstance.id + "#"
          val additionalKey =
            if (documents.keyProperty().value()) Some(ctx.dialect.name().value())
            else None
          InstanceElementParser(root).parse(
            path,
            encodedElementDefaultId(dialectInstance),
            map,
            nodeMapping,
            rootNode = true,
            givenAnnotations = None,
            additionalKey = additionalKey
          )
        case _ =>
          emptyElementWithViolation(s"Could not find node mapping for: ${mapping.encoded().value()}")
      }
    }
    result.getOrElse {
      emptyElementWithViolation("Could not find root document mapping from dialect")
    }
  }

  protected def emptyElementWithViolation(msg: String)(implicit ctx: DialectInstanceContext): DialectDomainElement = {
    val empty = DialectDomainElement(map).withId(root.location)
    ctx.eh.violation(DialectError, empty.id, msg, map.location)
    empty
  }
}
