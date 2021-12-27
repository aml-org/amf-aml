package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{AnnotationMapping, DialectDomainElement}
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.parse.instances.parser.{ElementPropertyParser, InstanceNodeParser}
import amf.aml.internal.registries.AMLRegistry
import amf.aml.internal.render.emitters.instances.{
  DefaultNodeMappableFinder,
  DialectNodeEmitter,
  NodeFieldEmitters,
  NodeMappableFinder
}
import amf.aml.internal.semantic.SemanticExtensionHelper.{findAnnotationMapping, findSemanticExtension}
import amf.core.client.scala.config.RenderOptions
import amf.core.client.scala.model.domain.extensions.{CustomDomainProperty, DomainExtension}
import amf.core.client.scala.parse.document.{
  EmptyFutureDeclarations,
  ParserContext,
  SyamlParsedDocument,
  UnspecifiedReference
}
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.parser.{ParseConfiguration, Root}
import amf.core.internal.render.SpecOrdering
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances._
import org.yaml.model.YDocument.PartBuilder
import org.yaml.model.{YDocument, YMap, YMapEntry, YNode}

class SemanticExtensionsFacade private (val registry: AMLRegistry) {

  def parse(extensionName: String,
            parentTypes: Seq[String],
            ast: YMapEntry,
            ctx: ParserContext,
            extensionId: String): Option[DomainExtension] = {
    findExtensionMapping(extensionName, parentTypes).map {
      case (mapping, dialect) => parseSemanticExtension(dialect, mapping, ast, ctx, extensionId, extensionName)
    }
  }

  def render(key: String,
             extension: DomainExtension,
             parentTypes: Seq[String],
             ordering: SpecOrdering,
             renderOptions: RenderOptions,
             nodeMappableFinder: NodeMappableFinder) = {
    val maybeName = Option(extension.definedBy).flatMap(_.name.option())
    maybeName
      .flatMap(name => findExtensionMapping(name, parentTypes))
      .map {
        case (mapping, dialect) =>
          NodeFieldEmitters(extension,
                            mapping,
                            dialect.references,
                            dialect,
                            ordering,
                            None,
                            None,
                            false,
                            Nil,
                            renderOptions,
                            registry,
                            Some(key))(nodeMappableFinder).emitField(mapping.toField())
      }
      .getOrElse(Nil)
  }

  private def findExtensionMapping(name: String, parentTypes: Seq[String]): Option[(AnnotationMapping, Dialect)] = {
    findExtensionDialect(name).flatMap { dialect =>
      findSemanticExtension(dialect, name)
        .map(findAnnotationMapping(dialect, _))
        .filter(_.appliesTo(parentTypes))
        .map(mapping => (mapping, dialect))
    }
  }

  def findAnnotation(extensionName: String): Option[AnnotationMapping] = {
    findExtensionDialect(extensionName).flatMap { dialect =>
      findSemanticExtension(dialect, extensionName).map(findAnnotationMapping(dialect, _))
    }
  }

  def findAnnotationMappingByExtension(extensionName: String, dialect: Dialect): Option[AnnotationMapping] =
    findSemanticExtension(dialect, extensionName).map { extension =>
      findAnnotationMapping(dialect, extension)
    }

  def findExtensionDialect(name: String): Option[Dialect] = findExtensionDialect.runCached(name)

  def parseSemanticExtension(dialect: Dialect,
                             mapping: AnnotationMapping,
                             ast: YMapEntry,
                             ctx: ParserContext,
                             extensionId: String,
                             extensionName: String): DomainExtension = {

    implicit val instanceCtx: DialectInstanceContext = instanceContext(dialect, ctx)

    val value = ast.value

    val instanceElement: DialectDomainElement = parseAnnotation(mapping, ast, extensionId)

    val property                   = createCustomDomainProperty(instanceElement, extensionName, value)
    val extension: DomainExtension = createDomainExtension(extensionName, value, extensionId, property)
    mergeAnnotationIntoExtension(instanceElement, extension)
  }

  private def instanceContext(dialect: Dialect, ctx: ParserContext) = {
    val nextCtx = ctx.copy(futureDeclarations = EmptyFutureDeclarations())
    new DialectInstanceContext(dialect, DefaultNodeMappableFinder(Seq(dialect)), nextCtx)
  }

  private def mergeAnnotationIntoExtension(instanceElement: DialectDomainElement, extension: DomainExtension) = {
    val fields = instanceElement.fields.fields()
    fields.foldLeft(extension) { (acc, curr) =>
      acc.set(curr.field, curr.element, curr.value.annotations)
    }
  }

  private def createDomainExtension(key: String, value: YNode, id: String, property: CustomDomainProperty) = {
    val extension = DomainExtension()
      .withId(id)
      .withDefinedBy(property)
      .withName(key)
      .add(Annotations(value))
    extension
  }

  private def createCustomDomainProperty(instanceElement: DialectDomainElement, key: String, value: YNode) = {
    CustomDomainProperty(Annotations(value)).withId(instanceElement.id).withName(key, Annotations())
  }

  private def parseAnnotation(mapping: AnnotationMapping, ast: YMapEntry, extensionId: String)(
      implicit ctx: DialectInstanceContext) = {
    // TODO: improve, shouldn't have to create fake root node
    val fakeRoot        = Root(SyamlParsedDocument(YDocument(YMap.empty)), "", "", Seq.empty, UnspecifiedReference, "{}")
    val instanceElement = DialectDomainElement().withId("someId")
    val nodeParser      = InstanceNodeParser(fakeRoot)
    val propertyParser  = new ElementPropertyParser(fakeRoot, YMap.empty, nodeParser.parse)
    propertyParser.parse(extensionId, ast, mapping, instanceElement)
    ctx.futureDeclarations.resolve()
    instanceElement
  }

  private val findExtensionDialect = CachedFunction.fromMonadic { name =>
    registry.findExtension(name)
  }
}

object SemanticExtensionsFacade {
  def apply(registry: AMLRegistry): SemanticExtensionsFacade = new SemanticExtensionsFacade(registry)
  // This method will assume that the parse configuration contains an AMLRegistry
  def apply(config: ParseConfiguration): SemanticExtensionsFacade =
    apply(config.registryContext.getRegistry.asInstanceOf[AMLRegistry])
}
