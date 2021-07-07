package amf.aml.internal.semantic

import amf.aml.client.scala.model.domain.SemanticExtension
import amf.core.client.scala.model.domain.DomainElement
import org.mulesoft.common.collections.FilterType
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances.seqMonad

object SemanticExtensionFinder {
  def apply(extensions: Seq[DomainElement], extractor: SearchFieldExtractor): SemanticExtensionFinder = {
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
