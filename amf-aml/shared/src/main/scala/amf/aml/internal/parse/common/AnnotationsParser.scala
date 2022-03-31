package amf.aml.internal.parse.common

import amf.aml.internal.parse.DynamicExtensionParser
import amf.aml.internal.parse.instances.DialectInstanceContext
import amf.aml.internal.parse.vocabularies.VocabularyDeclarations
import amf.aml.internal.validate.DialectValidations.MissingVocabulary
import amf.core.client.scala.model.domain.{AmfObject, DomainElement}
import amf.core.client.scala.model.domain.extensions.{CustomDomainProperty, DomainExtension}
import amf.core.client.scala.parse.document.ParserContext
import amf.core.internal.metamodel.domain.DomainElementModel.CustomDomainProperties
import amf.core.internal.metamodel.domain.extensions.DomainExtensionModel
import amf.core.internal.parser.domain.Annotations
import amf.core.internal.plugins.document.graph.parser.FlattenedUnitGraphParser.ctx
import org.yaml.model.{YMap, YMapEntry}

import scala.util.matching.Regex
import scala.util.{Failure, Success}

object AnnotationsParser {

  def parseAnnotations(ast: YMap, node: AmfObject, declarations: VocabularyDeclarations)(
      implicit ctx: ParserContext): Any = {

    computeAnnotationInfo(ast) flatMap { ai =>
      computeSemanticExtensionParser(ctx, ai.key)
        .flatMap { parser =>
          val id    = node.id + s"${ai.prefix.map(_ + "/").getOrElse("/")}${ai.suffix}"
          val types = node.meta.`type`.map(_.iri())
          parser.parse(types, ai.entry, ctx, id)
        }
        .orElse(parseRegularAnnotation(node, declarations, ctx, ai))
        .map(extension => node.add(CustomDomainProperties, extension))
    }
  }

  private def computeSemanticExtensionParser(ctx: ParserContext, key: String) = {
    ctx match {
      case diCtx: DialectInstanceContext => Some(diCtx.extensionsFacadeBuilder.extensionName(key))
      case _                             => None
    }
  }

  private def parseRegularAnnotation(node: AmfObject,
                                     declarations: VocabularyDeclarations,
                                     ctx: ParserContext,
                                     ai: AnnotationInfo): Option[DomainExtension] = {
    val value = ai.entry.value
    val key   = ai.originalKey
    declarations.resolveExternalNamespace(ai.prefix, ai.suffix) match {
      case Success(propertyId) =>
        val id               = node.id + s"${ai.prefix.map(_ + "/").getOrElse("/")}$ai.suffix"
        val parsedAnnotation = DynamicExtensionParser(value, Some(id))(ctx).parse()
        val property         = CustomDomainProperty(Annotations(value)).withId(propertyId).withName(key, Annotations())
        val extension = DomainExtension()
          .withId(id)
          .set(DomainExtensionModel.Extension, parsedAnnotation, Annotations.inferred())
          .withDefinedBy(property)
          .withName(key)
          .add(Annotations(value))
        Some(extension)
      case Failure(ex) =>
        declarations.usedVocabs.get(ai.prefix.getOrElse("")) match {
          case Some(vocabulary) =>
            val id               = node.id + (if (node.id.endsWith("/") || node.id.endsWith("#")) "" else "/") + s"${ai.prefix.map(_ + "/").getOrElse("/")}$ai.suffix"
            val parsedAnnotation = DynamicExtensionParser(value, Some(id))(ctx).parse()
            val base             = vocabulary.base.value()
            val propertyId       = if (base.endsWith("#") || base.endsWith("/")) base + ai.suffix else base + "/" + ai.suffix
            val property         = CustomDomainProperty(Annotations(value)).withId(propertyId).withName(ai.key)
            val extension = DomainExtension()
              .withId(id)
              .set(DomainExtensionModel.Extension, parsedAnnotation, Annotations.inferred())
              .withDefinedBy(property)
              .withName(ai.key)
              .add(Annotations(value))
            Some(extension)
          case None =>
            ctx.eh.violation(MissingVocabulary, node.id, ex.getMessage, value)
            None
        }
    }
  }

  def computeAnnotationInfo(ast: YMap): Seq[AnnotationInfo] = ast.entries.flatMap { entry: YMapEntry =>
    val key: String                       = entry.key.as[String]
    val maybeBS: Option[BaseAndSeparator] = getKeyName(key)
    maybeBS.map { bs =>
      val ps = getPrefixAndSuffix(bs)
      AnnotationInfo(key, bs.base, ps.prefix, ps.suffix, entry)
    }
  }

  private def getKeyName(key: String): Option[BaseAndSeparator] = {
    val ramlLikeAnnotation: Regex = "[(](.+)[)]".r
    val oasLikeAnnotation: Regex  = "x-(.+)".r
    key match {
      case ramlLikeAnnotation(ramlValue) => Some(BaseAndSeparator(ramlValue, "\\."))
      case oasLikeAnnotation(oasValue)   => Some(BaseAndSeparator(oasValue, "-"))
      case _                             => None
    }
  }

  private def getPrefixAndSuffix(baseAndSeparator: BaseAndSeparator): PrefixAndSuffix =
    baseAndSeparator.base.split(baseAndSeparator.separator) match {
      case Array(prefix, suffix) => PrefixAndSuffix(Some(prefix), suffix)
      case Array(suffix)         => PrefixAndSuffix(None, suffix)
    }

  private case class BaseAndSeparator(base: String, separator: String)
  private case class PrefixAndSuffix(prefix: Option[String], suffix: String)
  case class AnnotationInfo(originalKey: String, key: String, prefix: Option[String], suffix: String, entry: YMapEntry)

}
