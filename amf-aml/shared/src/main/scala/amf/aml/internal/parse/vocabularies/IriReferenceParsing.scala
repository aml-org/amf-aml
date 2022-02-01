package amf.aml.internal.parse.vocabularies

import amf.core.client.scala.model.domain.AmfScalar
import amf.core.internal.parser.domain.Annotations
import org.yaml.model.YPart

trait IriReferenceParsing {

  protected def parseIriAlias(iris: Seq[String],
                              computeAliasedTerm: String => Option[String],
                              onError: String => Unit) = {
    iris.flatMap { term =>
      computeAliasedTerm(term) match {
        case Some(v) => Some(AmfScalar(v, Annotations.synthesized()))
        case None =>
          onError(term)
          None
      }
    }
  }
}
