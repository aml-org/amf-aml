package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.{AnnotationMapping, DialectDomainElement}
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.parse.instances.parser.{ElementPropertyParser, InstanceElementParser}
import amf.aml.internal.render.emitters.instances.DefaultNodeMappableFinder
import amf.aml.internal.semantic.SemanticExtensionHelper.{findAnnotationMapping, findSemanticExtension}
import amf.aml.internal.semantic.SemanticExtensionOps.findExtensionMapping
import amf.core.client.scala.model.domain.extensions.{CustomDomainProperty, DomainExtension}
import amf.core.client.scala.parse.document.{
  EmptyFutureDeclarations,
  ParserContext,
  SyamlParsedDocument,
  UnspecifiedReference
}
import amf.core.internal.parser.Root
import amf.core.internal.parser.domain.Annotations
import org.yaml.model.{YDocument, YMap, YMapEntry, YNode}

class SemanticExtensionParser(finder: ExtensionDialectFinder, specAnnotationValidator: AnnotationSchemaValidator) {

  def parse(
      extensionName: String,
      parentTypes: Seq[String],
      ast: YMapEntry,
      ctx: ParserContext,
      extensionId: String
  ): Option[DomainExtension] = {
    findExtensionMapping(extensionName, parentTypes, finder).map { case (mapping, dialect) =>
      specAnnotationValidator.validate(extensionName, ast.key, ctx.eh)
      parseSemanticExtension(dialect, mapping, ast, ctx, extensionId, extensionName)
    }
  }

  private def parseSemanticExtension(
      dialect: Dialect,
      mapping: AnnotationMapping,
      ast: YMapEntry,
      ctx: ParserContext,
      extensionId: String,
      extensionName: String
  ): DomainExtension = {

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

  private def parseAnnotation(mapping: AnnotationMapping, ast: YMapEntry, extensionId: String)(implicit
      ctx: DialectInstanceContext
  ) = {
    // TODO: improve, shouldn't have to create fake root node
    val fakeRoot = Root(SyamlParsedDocument(YDocument(YMap.empty)), "", "", Seq.empty, UnspecifiedReference, "{}")
    val instanceElement = DialectDomainElement().withId("someId")
    val nodeParser      = InstanceElementParser(fakeRoot)
    val propertyParser  = new ElementPropertyParser(fakeRoot, YMap.empty, nodeParser.parse)
    propertyParser.parse(extensionId, ast, mapping, instanceElement)
    ctx.futureDeclarations.resolve()
    instanceElement
  }
}
