package amf.aml.internal.parse.vocabularies
import amf.core.client.scala.model.domain.DomainElement
import amf.core.client.scala.parse.document.{ParserContext, SyamlBasedParserErrorHandler}
import amf.aml.client.scala.model.document.Vocabulary
import amf.aml.client.scala.model.domain.{ClassTerm, PropertyTerm}
import amf.aml.internal.parse.common.SyntaxErrorReporter
import amf.aml.internal.validate.DialectValidations
import amf.core.internal.plugins.syntax.SYamlAMFParserErrorHandler
import org.yaml.model.YPart

class VocabularyContext(private val wrapped: ParserContext, private val ds: Option[VocabularyDeclarations] = None)
    extends SyamlBasedParserErrorHandler(wrapped.rootContextDocument,
                                         wrapped.refs,
                                         wrapped.futureDeclarations,
                                         wrapped.config)
    with VocabularySyntax
    with SyntaxErrorReporter {

  var imported: Map[String, Vocabulary] = Map()

  def registerVocabulary(alias: String, vocabulary: Vocabulary): Unit = {
    imported += (alias -> vocabulary)
  }

  var pendingLocal: Seq[(String, String, YPart, Boolean)] = Nil

  private def isDuplicated(iri: String): Boolean = {
    val isDuplicate = terms().exists(_.id == iri)
    if (isDuplicate) {
      eh.violation(DialectValidations.DuplicateTerm, iri, s"'$iri' term cannot be both a class term and property term")
    }
    isDuplicate
  }

  def register(alias: String, classTerm: ClassTerm): Unit = {
    if (!isDuplicated(classTerm.id)) {
      pendingLocal = pendingLocal.filter(_._1 != classTerm.id)
      declarations.classTerms += (alias -> classTerm)
    }
  }

  def register(alias: String, propertyTerm: PropertyTerm): Unit = {
    if (!isDuplicated(propertyTerm.id)) {
      pendingLocal = pendingLocal.filter(_._1 != propertyTerm.id)
      declarations.propertyTerms += (alias -> propertyTerm)
    }
  }

  def resolvePropertyTermAlias(base: String,
                               propertyTermAlias: String,
                               where: YPart,
                               strictLocal: Boolean): Option[String] = {
    if (propertyTermAlias.contains(".")) {
      val prefix = propertyTermAlias.split("\\.").head
      val value  = propertyTermAlias.split("\\.").last
      declarations.externals.get(prefix) match {
        case Some(external) => Some(s"${external.base.value()}$value")
        case None =>
          declarations.libraries.get(prefix) match {
            case Some(vocab: VocabularyDeclarations) => vocab.getPropertyTermId(value)
            case _                                   => None
          }
      }
    } else {
      val local = s"$base$propertyTermAlias"
      declarations.getPropertyTermId(propertyTermAlias) match {
        case Some(_) => // ignore
        case None =>
          if (strictLocal) {
            pendingLocal ++= Seq((local, propertyTermAlias, where, true))
          }
      }
      Some(local)
    }
  }

  def resolveClassTermAlias(base: String, classTermAlias: String, where: YPart, strictLocal: Boolean): Option[String] = {
    if (classTermAlias.contains(".")) {
      val prefix = classTermAlias.split("\\.").head
      val value  = classTermAlias.split("\\.").last
      declarations.externals.get(prefix) match {
        case Some(external) => Some(s"${external.base.value()}$value")
        case None =>
          declarations.libraries.get(prefix) match {
            case Some(vocab: VocabularyDeclarations) => vocab.getClassTermId(value)
            case _                                   => None
          }
      }
    } else {
      val local = s"$base$classTermAlias"
      declarations.getClassTermId(classTermAlias) match {
        case Some(_) => // ignore
        case None    => if (strictLocal) { pendingLocal ++= Seq((local, classTermAlias, where, false)) }
      }
      Some(local)
    }
  }

  val declarations: VocabularyDeclarations =
    ds.getOrElse(new VocabularyDeclarations(errorHandler = eh, futureDeclarations = futureDeclarations))

  def terms(): Seq[DomainElement] = declarations.classTerms.values.toSeq ++ declarations.propertyTerms.values.toSeq
}
