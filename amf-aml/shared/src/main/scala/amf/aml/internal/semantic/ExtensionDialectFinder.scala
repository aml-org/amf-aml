package amf.aml.internal.semantic

import amf.aml.client.scala.model.document.Dialect
import amf.aml.internal.registries.AMLRegistry
import org.mulesoft.common.core.CachedFunction
import org.mulesoft.common.functional.MonadInstances.optionMonad

trait ExtensionDialectFinder {
  def find(name: String): Option[Dialect]
}

case class CachedExtensionDialectFinder(registry: AMLRegistry) extends ExtensionDialectFinder {

  private val findExtensionDialect = CachedFunction.fromMonadic { name =>
    registry.findExtension(name)
  }
  override def find(name: String): Option[Dialect] = findExtensionDialect.runCached(name)
}
