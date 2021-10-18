package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect
import amf.aml.client.scala.model.domain.AnnotationMapping
import amf.aml.internal.registries.AMLRegistry
import amf.core.client.scala.model.domain.extensions.DomainExtension
import amf.core.internal.parser.ParseConfiguration
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances._
import org.yaml.model.{YMapEntry, YNode}

class SemanticExtensionsFacade private (registry: AMLRegistry) {

  def findAnnotationMappingByExtension(extensionName: String, dialect: Dialect): Option[AnnotationMapping] =
    SemanticExtensionHelper.findSemanticExtension(extensionName).map { extension =>
      SemanticExtensionHelper.findAnnotationMapping(dialect, extension)
    }

  def parseSemanticExtension(mapping: AnnotationMapping, ast: YMapEntry): DomainExtension = {

    val field = mapping.toField
    // TODO

    null
  }

  def findExtensionDialect(name: String): Option[Dialect] = findExtensionDialect.runCached(name)

  private val findExtensionDialect = CachedFunction.fromMonadic { name: String =>
    registry.findExtension(name)
  }
}

object SemanticExtensionsFacade {
  def apply(registry: AMLRegistry): SemanticExtensionsFacade = new SemanticExtensionsFacade(registry)
  // This method will assume that the parse configuration contains an AMLRegistry
  def apply(config: ParseConfiguration): SemanticExtensionsFacade =
    apply(config.registryContext.getRegistry.asInstanceOf[AMLRegistry])
}
