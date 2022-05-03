package amf.aml.internal.parse.instances

import amf.core.client.scala.errorhandling.AMFErrorHandler
import amf.core.client.scala.parse.document.EmptyFutureDeclarations
import amf.core.internal.parser.domain.{FutureDeclarations, SearchScope}
import amf.aml.client.scala.model.domain.{DialectDomainElement, NodeMappable}
import amf.aml.internal.parse.vocabularies.VocabularyDeclarations

class DialectInstanceDeclarations(
    var dialectDomainElements: Map[String, DialectDomainElement] = Map(),
    errorHandler: AMFErrorHandler,
    futureDeclarations: FutureDeclarations
) extends VocabularyDeclarations(Map(), Map(), Map(), Map(), Map(), errorHandler, futureDeclarations)
    with NodeMappableHelper {

  /** Get or create specified library. */
  override def getOrCreateLibrary(alias: String): DialectInstanceDeclarations = {
    libraries.get(alias) match {
      case Some(lib: DialectInstanceDeclarations) => lib
      case _ =>
        val result =
          new DialectInstanceDeclarations(errorHandler = errorHandler, futureDeclarations = EmptyFutureDeclarations())
        libraries = libraries + (alias -> result)
        result
    }
  }

  def registerDialectDomainElement(
      name: String,
      dialectDomainElement: DialectDomainElement
  ): DialectInstanceDeclarations = {
    dialectDomainElements += (name -> dialectDomainElement)
    if (!dialectDomainElement.isUnresolved) {
      futureDeclarations.resolveRef(name, dialectDomainElement)
    }
    this
  }

  def findDialectDomainElement(
      key: String,
      nodeMapping: NodeMappable,
      scope: SearchScope.Scope
  ): Option[DialectDomainElement] = {
    val nodeMappingIds = allNodeMappingIds(nodeMapping)
    findForType(key, _.asInstanceOf[DialectInstanceDeclarations].dialectDomainElements, scope) collect {
      case dialectDomainElement: DialectDomainElement if nodeMappingIds.contains(dialectDomainElement.definedBy.id) =>
        dialectDomainElement
    }
  }

  def findAnyDialectDomainElement(key: String, scope: SearchScope.Scope): Option[DialectDomainElement] = {
    findForType(key, _.asInstanceOf[DialectInstanceDeclarations].dialectDomainElements, scope) collect {
      case dialectDomainElement: DialectDomainElement => dialectDomainElement
    }
  }

  override def declarables(): Seq[DialectDomainElement] = dialectDomainElements.values.toSet.toSeq
}
