package amf.aml.internal.utils

import amf.aml.client.scala.model.document.Dialect
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.core.internal.registries.AMFRegistry

import scala.collection.immutable

object DialectHelper {

  def findDialect(nameAndVersion: String, registry: AMFRegistry): immutable.Seq[Dialect] =
    registry.getPluginsRegistry.rootParsePlugins.collect {
      case plugin: AMLDialectInstanceParsingPlugin if plugin.dialect.nameAndVersion() == nameAndVersion =>
        plugin.dialect
    }

}
