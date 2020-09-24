package amf.plugins.document.vocabularies.parser.dialects

import amf.core.annotations.Aliases
import amf.core.model.document.{BaseUnit, DeclaresModel, RecursiveUnit}
import amf.core.parser.{ParsedReference, Reference}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectFragment, Vocabulary}
import amf.plugins.document.vocabularies.model.domain.External
import amf.validation.DialectValidations.DialectError
import org.yaml.model.{YMap, YMapEntry, YScalar, YType}
import amf.plugins.document.vocabularies.parser.dialects.DialectAstOps._

case class DialectsReferencesParser(dialect: Dialect, map: YMap, references: Seq[ParsedReference])(
    implicit ctx: DialectContext) {

  def parse(location: String): ReferenceDeclarations = {
    val result = ReferenceDeclarations()

    references.foreach {
      case ParsedReference(f: DialectFragment, origin: Reference, None) => result += (origin.url, f)
      case ParsedReference(r: RecursiveUnit, origin: Reference, _)      => result += (origin.url, r)
      case _                                                            =>
    }

    parseLibraries(dialect, result, location)
    parseExternals(result, location)
    result
  }

  private def parseLibraries(dialect: Dialect, result: ReferenceDeclarations, id: String): Unit = {
    map.key(
        "uses",
        entry =>
          entry.value
            .as[YMap]
            .entries
            .foreach(e => {
              val alias: String = e.key.as[YScalar].text
              val url: String   = library(e)
              target(url).foreach {
                case module: Vocabulary =>
                  collectAlias(dialect, alias -> (module.base.value(), url))
                  result += (alias, module)
                case module: DeclaresModel =>
                  collectAlias(dialect, alias -> (module.id, url))
                  result += (alias, module)
                case other =>
                  ctx.recursiveDeclarations.get(url) match {
                    case Some(r: RecursiveUnit) =>
                      result += (alias, r)
                    case None =>
                      ctx.eh.violation(DialectError, id, s"Expected vocabulary module but found: $other", e) // todo Uses should only reference modules...
                  }
              }
            })
    )
  }

  private def target(url: String): Option[BaseUnit] =
    references.find(r => r.origin.url.equals(url)).map(_.unit)

  private def library(e: YMapEntry): String = e.value.tagType match {
    case YType.Include => e.value.as[YScalar].text
    case _             => e.value
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

  private def parseExternals(result: ReferenceDeclarations, id: String): Unit = {
    map.key(
        "external",
        entry =>
          entry.value
            .as[YMap]
            .entries
            .foreach(e => {
              val alias: String = e.key.as[YScalar].text
              val base: String  = e.value
              val external      = External()
              result += external.withAlias(alias).withBase(base)
            })
    )
  }
}
