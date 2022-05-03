package amf.rdf.internal

import amf.core.client.scala.model.domain.{AmfElement, Annotation, DomainElement, ExternalSourceElement}
import amf.core.client.scala.parse.document.{EmptyFutureDeclarations, ParserContext}
import amf.core.internal.parser.ParseConfiguration
import amf.core.internal.parser.domain.FutureDeclarations
import amf.core.internal.rdf.SerializableAnnotationsFacade

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class RdfParserContext(
    rootContextDocument: String = "",
    futureDeclarations: FutureDeclarations = EmptyFutureDeclarations(),
    config: ParseConfiguration
) extends ParserContext(rootContextDocument, Seq.empty, futureDeclarations, config) {

  val unresolvedReferences: mutable.Map[String, Seq[DomainElement]] = mutable.Map[String, Seq[DomainElement]]()
  val unresolvedExtReferencesMap: mutable.Map[String, ExternalSourceElement] =
    mutable.Map[String, ExternalSourceElement]()

  val referencesMap: mutable.Map[String, DomainElement] = mutable.Map[String, DomainElement]()

  val collected: ListBuffer[Annotation] = ListBuffer()

  var nodes: Map[String, AmfElement] = Map()

  val annotationsFacade: SerializableAnnotationsFacade = config.serializableAnnotationsFacade
}
