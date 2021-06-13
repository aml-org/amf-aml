package amf.plugins.document.vocabularies.parser.instances

import amf.core.internal.annotations.{Aliases, LexicalInformation}
import amf.core.client.scala.model.document.{BaseUnit, DeclaresModel}
import amf.core.client.scala.model.domain.{AmfObject, Annotation}
import amf.core.internal.parser.domain.Annotations
import amf.plugins.document.vocabularies.annotations.AliasesLocation
import amf.plugins.document.vocabularies.model.document.{
  DialectInstance,
  DialectInstanceFragment,
  DialectInstanceLibrary
}
import amf.plugins.document.vocabularies.model.domain.External
import amf.validation.DialectValidations.DialectError
import org.yaml.model.{YMap, YMapEntry, YScalar, YType}
import amf.core.client.scala.parse.document._
import amf.core.internal.parser.YMapOps

import scala.collection.mutable

case class DialectInstanceReferencesParser(dialectInstance: BaseUnit, map: YMap, references: Seq[ParsedReference])(
    implicit ctx: DialectInstanceContext) {

  def parse(location: String): ReferenceCollector[AmfObject] = {
    val result = CallbackReferenceCollector(DialectInstanceRegister())
    parseLibraries(dialectInstance, result, location)
    parseExternals(result, location)
    references.foreach {
      case ParsedReference(f: DialectInstanceFragment, origin: Reference, None) => result += (origin.url, f)
      case _                                                                    =>
    }
    if (ctx.isPatch) {
      references.foreach {
        case ParsedReference(f: DialectInstance, origin: Reference, None) => result += (origin.url, f)
        case _                                                            =>
      }
    }

    result
  }

  private def target(url: String): Option[BaseUnit] =
    references.find(r => r.origin.url.equals(url)).map(_.unit)

  private def parseLibraries(dialectInstance: BaseUnit, result: ReferenceCollector[AmfObject], id: String): Unit = {
    val parsedLibraries: mutable.Set[String] = mutable.Set()
    map.key(
        "uses",
        entry => {
          val annotation: Annotation =
            AliasesLocation(
                Annotations(entry.key).find(classOf[LexicalInformation]).map(_.range.start.line).getOrElse(0))
          dialectInstance.annotations += annotation
          entry.value
            .as[YMap]
            .entries
            .foreach(e => {
              val alias: String = e.key.as[YScalar].text
              val url: String   = library(e)
              target(url).foreach {
                case module: DeclaresModel =>
                  parsedLibraries += url
                  collectAlias(dialectInstance, alias -> (module.id, url))
                  result += (alias, module)
                case other =>
                  ctx.eh.violation(DialectError, id, s"Expected vocabulary module but found: '$other'", e) // todo Uses should only reference modules...
              }
            })
        }
    )
    // Parsing $refs to libraries
    references.foreach {
      case ParsedReference(lib: DialectInstanceLibrary, _, _)
          if !parsedLibraries.contains(lib.location().getOrElse(lib.id)) =>
        result += (lib.id, lib)
      case _ => // ignore
    }
  }

  private def library(e: YMapEntry): String = e.value.tagType match {
    case YType.Include => e.value.as[YScalar].text
    case YType.Map if e.value.as[YMap].key("$include").isDefined =>
      e.value.as[YMap].key("$include").get.value.as[String]
    case _ => e.value
  }

  private def parseExternalEntry(result: ReferenceCollector[AmfObject], entry: YMapEntry): Unit = {
    entry.value
      .as[YMap]
      .entries
      .foreach(e => {
        val alias: String = e.key.as[YScalar].text
        val base: String  = e.value
        val external      = External()
        result += (alias, external.withAlias(alias).withBase(base))
      })
  }
  private def parseExternals(result: ReferenceCollector[AmfObject], id: String): Unit = {
    map.key(
        "external",
        entry => parseExternalEntry(result, entry)
    )

    map.key(
        "$external",
        entry => parseExternalEntry(result, entry)
    )
  }

  private def collectAlias(aliasCollectorUnit: BaseUnit,
                           alias: (Aliases.Alias, (Aliases.FullUrl, Aliases.RelativeUrl))): BaseUnit = {
    aliasCollectorUnit.annotations.find(classOf[Aliases]) match {
      case Some(aliases) =>
        aliasCollectorUnit.annotations.reject(_.isInstanceOf[Aliases])
        aliasCollectorUnit.add(aliases.copy(aliases = aliases.aliases + alias))
      case None => aliasCollectorUnit.add(Aliases(Set(alias)))
    }
  }
}
