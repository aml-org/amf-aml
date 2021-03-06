package amf.plugins.document.vocabularies.parser.instances

import amf.core.parser._
import amf.plugins.document.vocabularies.model.document.Dialect
import amf.plugins.document.vocabularies.model.domain.{
  DialectDomainElement,
  DocumentMapping,
  NodeMappable,
  PublicNodeMapping
}
import amf.plugins.document.vocabularies.parser.common.{DeclarationContext, SyntaxErrorReporter}
import org.yaml.model.{IllegalTypeHandler, YMap, YNode, YScalar, YType}

class DialectInstanceContext(var dialect: Dialect,
                             private val wrapped: ParserContext,
                             private val ds: Option[DialectInstanceDeclarations] = None)
    extends ParserContext(wrapped.rootContextDocument, wrapped.refs, wrapped.futureDeclarations, wrapped.eh)
    with DeclarationContext
    with SyntaxErrorReporter {

  var isPatch: Boolean                                           = false
  var nestedDialects: Seq[Dialect]                               = Nil
  val libraryDeclarationsNodeMappings: Map[String, NodeMappable] = parseDeclaredNodeMappings("library")
  val rootDeclarationsNodeMappings: Map[String, NodeMappable]    = parseDeclaredNodeMappings("root")

  def computeRootProps: Set[String] = {
    val declarationProps: Set[String] = Option(dialect.documents()).flatMap(_.declarationsPath().option()) match {
      case Some(declarationsPath) => Set(declarationsPath.split("/").head)
      case _ =>
        (
            Option(dialect.documents())
              .flatMap(d => Option(d.root()))
              .map(_.declaredNodes().map(_.name().value()))
              .getOrElse(Seq()) ++
              Option(dialect.documents())
                .flatMap(d => Option(d.library()))
                .map(_.declaredNodes().map(_.name().value()))
                .getOrElse(Seq())
        ).toSet
    }
    declarationProps ++ Seq("uses", "external").toSet
  }

  var rootProps: Set[String] = computeRootProps

  globalSpace = wrapped.globalSpace

  def forPatch(): DialectInstanceContext = {
    isPatch = true
    this
  }

  def registerJsonPointerDeclaration(pointer: String, declared: DialectDomainElement): Unit =
    globalSpace.update(pointer, declared)

  def findJsonPointer(pointer: String): Option[DialectDomainElement] = globalSpace.get(pointer) match {
    case Some(e: DialectDomainElement) => Some(e)
    case _                             => None
  }

  override val declarations: DialectInstanceDeclarations =
    ds.getOrElse(new DialectInstanceDeclarations(errorHandler = eh, futureDeclarations = futureDeclarations))

  def withCurrentDialect[T](tmpDialect: Dialect)(k: => T): T = {
    val oldDialect = dialect
    dialect = tmpDialect
    rootProps = computeRootProps
    val res = k
    dialect = oldDialect
    rootProps = computeRootProps
    res
  }

  protected def parseDeclaredNodeMappings(documentType: String): Map[String, NodeMappable] = {
    val declarations: Seq[(String, NodeMappable)] = Option(dialect.documents())
      .flatMap { documents =>
        // document mappings for root and libraries, everything that declares something
        val documentMappings: Option[DocumentMapping] = if (documentType == "root") {
          Option(documents.root())
        } else {
          Option(documents.library())
        }
        documentMappings.map { mapping =>
          mapping.declaredNodes() map { declaration: PublicNodeMapping =>
            findNodeMapping(declaration.mappedNode().value()) map { nodeMapping =>
              (declaration.name().value(), nodeMapping)
            }
          } collect { case Some(res: (String, NodeMappable)) => res }
        }
      }
      .getOrElse(Nil)

    declarations.foldLeft(Map[String, NodeMappable]()) {
      case (acc, (name, mapping)) =>
        acc + (name -> mapping)
    }
  }

  def findNodeMapping(mappingId: String): Option[NodeMappable] = {
    dialect.declares.collectFirst {
      case mapping: NodeMappable if mapping.id == mappingId => mapping
    }
  }

  private def isInclude(node: YNode) = node.tagType == YType.Include

  private def isIncludeMap(node: YNode): Boolean = {
    node.asOption[YMap].flatMap(_.key("$include")).isDefined
  }

  def link(node: YNode)(implicit errorHandler: IllegalTypeHandler): Either[String, YNode] = {
    node match {
      case _ if isInclude(node)    => Left(node.as[YScalar].text)
      case _ if isIncludeMap(node) => Left(node.as[YMap].key("$include").get.value.as[String])
      case _                       => Right(node)
    }
  }
}
