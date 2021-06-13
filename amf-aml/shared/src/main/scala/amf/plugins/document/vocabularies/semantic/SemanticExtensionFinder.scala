package amf.plugins.document.vocabularies.semantic

import amf.core.client.scala.model.domain.DomainElement
import amf.plugins.document.vocabularies.model.domain.SemanticExtension
import org.mulesoft.common.collections.FilterType
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances.seqMonad

object SemanticExtensionFinder {
  def apply(extensions: Seq[DomainElement], extractor: SearchFieldExtractor) = {
    val semantic = extensions.filterType[SemanticExtension]
    new SemanticExtensionFinder(semantic, extractor)
  }
}

class SemanticExtensionFinder(private val extensions: Seq[SemanticExtension],
                              private val extractor: SearchFieldExtractor) {

  private val cache: CachedFunction[String, SemanticExtension, Seq] = CachedFunction.fromMonadic { uriOrString =>
    extensions
      .collect {
        case extension if extractor.extractSearchField(extension).isDefined =>
          (extension, extractor.extractSearchField(extension).get)
      }
      .filter {
        case (_, searchFieldValue) => searchFieldValue == uriOrString
      }
      .map(_._1)
  }

  def find(uri: String): Seq[SemanticExtension] = cache.runCached(uri)
}
