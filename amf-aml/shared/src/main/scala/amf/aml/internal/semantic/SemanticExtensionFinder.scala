package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.SemanticExtension
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances.optionMonad

object SemanticExtensionFinder {
  def apply(extensions: Map[String, Dialect], extractor: SearchFieldExtractor): SemanticExtensionFinder = {
    new SemanticExtensionFinder(extensions, extractor)
  }
}

class SemanticExtensionFinder(val extensions: Map[String, Dialect], val extractor: SearchFieldExtractor) {

  private val cache: CachedFunction[String, (SemanticExtension, Dialect), Option] = CachedFunction.fromMonadic {
    uriOrString =>
      extensions
        .map {
          case (extensionName, dialect) =>
            (dialect.extensions().find(p => p.extensionName().value() == extensionName), dialect)
        }
        .collect { case (Some(extension), dialect) => (extension, dialect) }
        .find { case (extension, _) => extractor.extractSearchField(extension).contains(uriOrString) }
  }

  def find(uri: String): Option[(SemanticExtension, Dialect)] = cache.runCached(uri)
}
