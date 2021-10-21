package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{AnnotationMapping, DialectDomainElement}
import amf.aml.internal.parse.DynamicExtensionParser
import amf.aml.internal.parse.common.AnnotationsParser.AnnotationInfo
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.parse.instances.parser.{
  ElementPropertyParser,
  InstanceNodeParser,
  ObjectPropertyParser,
  ObjectUnionParser,
  SimpleObjectPropertyParser
}
import amf.aml.internal.registries.AMLRegistry
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.aml.internal.semantic.SemanticExtensionHelper.{findAnnotationMapping, findSemanticExtension}
import amf.core.client.scala.model.domain.{AmfObject, DomainElement}
import amf.core.client.scala.model.domain.extensions.{CustomDomainProperty, DomainExtension}
import amf.core.client.scala.parse.document.{ParserContext, SyamlParsedDocument, UnspecifiedReference}
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.parser.{ParseConfiguration, Root}
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances._
import org.yaml.model.{YDocument, YMap, YMapEntry, YNode}

class SemanticExtensionsFacade private (val registry: AMLRegistry) {

  def parse(extensionName: String,
            parentTypes: Seq[String],
            ast: YMapEntry,
            ctx: ParserContext,
            extensionId: String): Option[DomainExtension] = {
    findExtensionDialect(extensionName).flatMap { dialect =>
      findSemanticExtension(dialect, extensionName)
        .map(findAnnotationMapping(dialect, _)) match {
        case Some(mapping) if mapping.appliesTo(parentTypes) =>
          Some(parseSemanticExtension(dialect, mapping, ast, ctx, extensionId))
        case _ => None // TODO: throw warning?
      }
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
                             extensionId: String): DomainExtension = {

    implicit val instanceCtx: DialectInstanceContext = instanceContext(dialect, ctx)

    val key   = ast.key.as[String]
    val value = ast.value

    val instanceElement: DialectDomainElement = parseAnnotation(mapping, ast)

    val property                   = createCustomDomainProperty(instanceElement, key, value)
    val extension: DomainExtension = createDomainExtension(key, value, extensionId, property)
    mergeAnnotationIntoExtension(instanceElement, extension)
  }

  private def instanceContext(dialect: Dialect, ctx: ParserContext) = {
    new DialectInstanceContext(dialect, DefaultNodeMappableFinder(Seq(dialect)), ctx)
  }

  private def mergeAnnotationIntoExtension(instanceElement: DialectDomainElement, extension: DomainExtension) = {
    val fields = instanceElement.fields.fields()
    fields.foldLeft(extension) { (acc, curr) =>
      acc.set(curr.field, curr.element)
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

  private def parseAnnotation(mapping: AnnotationMapping, ast: YMapEntry)(implicit ctx: DialectInstanceContext) = {
    // TODO: improve, shouldn't have to create fake root node
    val fakeRoot        = Root(SyamlParsedDocument(YDocument(YMap.empty)), "", "", Seq.empty, UnspecifiedReference, "{}")
    val nodeParser      = InstanceNodeParser(fakeRoot)
    val instanceElement = DialectDomainElement().withId("someId")
    SimpleObjectPropertyParser.parse("default", ast, mapping, instanceElement, Map.empty, nodeParser.parse)
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
