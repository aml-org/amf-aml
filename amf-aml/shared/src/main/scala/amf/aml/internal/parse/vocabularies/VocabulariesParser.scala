package amf.aml.internal.parse.vocabularies

import amf.aml.client.scala.model.document.Vocabulary
import amf.aml.client.scala.model.domain._
import amf.aml.internal.metamodel.document.VocabularyModel
import amf.aml.internal.metamodel.domain.{ClassTermModel, ObjectPropertyTermModel}
import amf.aml.internal.parse.common.{DeclarationKey, DeclarationKeyCollector}
import amf.core.client.scala.model.DataType
import amf.core.client.scala.model.document.BaseUnit
import amf.core.client.scala.model.domain.{AmfArray, AmfScalar}
import amf.core.client.scala.parse.document.SyamlParsedDocument
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.domain.{Annotations, BaseSpecParser, DefaultArrayNode, ValueNode}
import amf.core.internal.parser.{Root, YMapOps}
import amf.core.internal.remote.Spec.AML
import org.yaml.model._

class VocabulariesParser(root: Root)(implicit override val ctx: VocabularyContext)
    extends BaseSpecParser
    with DeclarationKeyCollector {

  val map: YMap = root.parsed.asInstanceOf[SyamlParsedDocument].document.as[YMap]
  val vocabulary: Vocabulary = {
    val location = root.location
    val result   = Vocabulary(Annotations(map)).withLocation(location).withId(location)
    result.processingData.adopted(location + "#")
    result
  }

  def parseDocument(): BaseUnit = {
    parseName(vocabulary)
    parseBase(vocabulary)
    parseUsage(vocabulary)

    // closed node validation
    ctx.closedNode("vocabulary", vocabulary.id, map)

    val references = VocabulariesReferencesParser(map, root.references).parse(vocabulary.base.value())

    if (ctx.declarations.externals.nonEmpty)
      vocabulary.withExternals(ctx.declarations.externals.values.toSeq)

    parseClassTerms(map)
    parsePropertyTerms(map)

    val declarables = ctx.terms()
    val imported = ctx.imported map { case (alias, library) =>
      VocabularyReference()
        .withAlias(alias)
        .withReference(library.id)
        .withBase(library.base.value())
        .adopted(vocabulary.id)
    }
    if (imported.nonEmpty)
      vocabulary.withImports(imported.toSeq)
    addDeclarationsToModel(vocabulary, declarables)
    if (references.nonEmpty) vocabulary.withReferences(references.baseUnitReferences())
    // we raise exceptions for missing terms
    ctx.pendingLocal.foreach { case (term, alias, location, isProperty) =>
      if (isProperty) {
        ctx.missingPropertyTermWarning(term, vocabulary.id, location)
      } else {
        ctx.missingClassTermWarning(term, vocabulary.id, location)
      }
    }

    vocabulary.processingData.withSourceSpec(AML)
    vocabulary
  }

  private def parseUsage(vocabulary: Vocabulary) = {
    map.key(
      "usage",
      entry => {
        val value = ValueNode(entry.value)
        vocabulary.set(VocabularyModel.Usage, value.string(), Annotations(entry))
      }
    )
  }

  private def parseBase(vocabulary: Vocabulary) = {
    map.key("base") match {
      case Some(entry) => {
        val value = ValueNode(entry.value)
        vocabulary.set(VocabularyModel.Base, value.string(), Annotations(entry))
      }
      case None => ctx.missingBaseTermViolation(vocabulary.id, map)
    }
  }

  private def parseName(vocabulary: Vocabulary) = {
    map.key("vocabulary") match {
      case Some(entry) => {
        val value = ValueNode(entry.value)
        vocabulary.set(VocabularyModel.Name, value.string(), Annotations(entry))
      }
      case None => ctx.missingVocabularyTermWarning(vocabulary.id, map)
    }
  }

  private def parseClassTerms(map: YMap): Unit = {
    map.key(
      "classTerms",
      entry => {
        addDeclarationKey(DeclarationKey(entry))
        val classDeclarations = entry.value.as[YMap]
        classDeclarations.entries.foreach { classTermDeclaration =>
          ClassTermParser().parse(classTermDeclaration, vocabulary)
        }
      }
    )
  }

  private def parsePropertyTerms(map: YMap): Unit = {
    map.key(
      "propertyTerms",
      entry => {
        addDeclarationKey(DeclarationKey(entry))
        val classDeclarations = entry.value.as[YMap]
        classDeclarations.entries.foreach { propertyTermDeclaration =>
          PropertyTermParser().parse(propertyTermDeclaration, vocabulary)
        }
      }
    )
  }
}
