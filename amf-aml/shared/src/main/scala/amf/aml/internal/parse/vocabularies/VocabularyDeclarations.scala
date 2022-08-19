package amf.aml.internal.parse.vocabularies
import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.parse.document.EmptyFutureDeclarations
import amf.core.client.scala.vocabulary.Namespace
import amf.core.internal.parser.domain.{Declarations, FutureDeclarations, DotQualifiedNameExtractor}
import amf.aml.client.scala.model.document.Vocabulary
import amf.aml.client.scala.model.domain.{ClassTerm, External, PropertyTerm}

import scala.util.{Failure, Success, Try}

class VocabularyDeclarations(
    var externals: Map[String, External] = Map(),
    var classTerms: Map[String, ClassTerm] = Map(),
    var propertyTerms: Map[String, PropertyTerm] = Map(),
    var usedVocabs: Map[String, Vocabulary] = Map(),
    libs: Map[String, VocabularyDeclarations] = Map(),
    errorHandler: AMFErrorHandler,
    futureDeclarations: FutureDeclarations
) extends Declarations(libs, Map(), Map(), errorHandler, futureDeclarations, DotQualifiedNameExtractor) {

  def registerTerm(term: PropertyTerm): Unit = {
    if (!term.name.value().contains(".")) {
      propertyTerms += (term.name.value() -> term)
    }
  }

  def registerTerm(term: ClassTerm): Unit = {
    if (!term.name.value().contains(".")) {
      classTerms += (term.name.value() -> term)
    }
  }

  def registerUsedVocabulary(alias: String, vocab: Vocabulary): Unit = usedVocabs += (alias -> vocab)

  /** Get or create specified library. */
  override def getOrCreateLibrary(alias: String): VocabularyDeclarations = {
    libraries.get(alias) match {
      case Some(lib: VocabularyDeclarations) => lib
      case _ =>
        val result =
          new VocabularyDeclarations(errorHandler = errorHandler, futureDeclarations = EmptyFutureDeclarations())
        libraries = libraries + (alias -> result)
        result
    }
  }

  def getTermId(value: String): Option[String] = getPropertyTermId(value).orElse(getClassTermId(value))

  def getPropertyTermId(alias: String): Option[String] = {
    propertyTerms.get(alias) match {
      case Some(pt) => Some(pt.id)
      case None     => None
    }
  }

  def getClassTermId(alias: String): Option[String] = {
    classTerms.get(alias) match {
      case Some(ct) => Some(ct.id)
      case None     => None
    }
  }

  def resolveExternal(key: String): Option[String] = {
    if (key.contains(".")) {
      val prefix = key.split("\\.").head
      val value  = key.split("\\.").last
      externals.get(prefix).map(external => s"${external.base.value()}$value")
    } else {
      None
    }
  }

  def resolveExternalNamespace(prefix: Option[String], suffix: String): Try[String] = {
    prefix match {
      case Some(prefixString) =>
        resolveExternal(s"$prefixString.$suffix") match {
          case Some(resolvedPrefix) => Success(resolvedPrefix)
          case _                    => Failure(new Exception(s"Cannot resolve external prefix $prefixString"))
        }
      case _ => Success((Namespace.Data + suffix).iri())
    }
  }
}
