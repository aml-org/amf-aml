package amf.aml.client.scala

import amf.aml.client.scala.model.document.{Dialect, DialectInstance}
import amf.aml.client.scala.model.domain.SemanticExtension
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.aml.internal.semantic.SemanticExtensionHelper
import com.github.ghik.silencer.silent

import scala.collection.immutable

/** Contains methods to get information about the current state of the configuration */
class AMLConfigurationState private[amf] (protected val configuration: AMLConfiguration) {

  /**
    * Get all instances of registered dialects
    * @return a Seq of [[Dialect]]
    */
  def getDialects(): immutable.Seq[Dialect] = getDialectsByCondition(_ => true)

  /**
    * Find an instance of registered dialect with the provided name and version
    * @param name of the dialect to find
    * @return a Seq of [[Dialect]]
    */
  def getDialect(name: String): immutable.Seq[Dialect] = getDialectsByCondition(dialectNameFilter(name))

  /**
    * Find an instance of registered dialect with the provided name and version
    * @param name of the dialect to find
    * @param version of dialect to find
    * @return an Option of [[Dialect]]
    */
  def getDialect(name: String, version: String): Option[Dialect] =
    getDialectsByCondition(dialectNameAndVersionFilter(name, version)).headOption

  /**
    * Get all instances of SemanticExtensions present in the registered dialects
    * @return a Seq of [[SemanticExtension]]
    */
  def getExtensions(): Seq[SemanticExtension] = SemanticExtensionHelper.getExtensions(configuration)

  /**
    * Find all instances of semantic extensions in the provided dialect filtering by the param
    * @param uri of the propertyTerm of the semantic extension to search
    * @return a Seq of [[SemanticExtension]]
    */
  def findSemanticByPropertyTerm(uri: String): Option[(SemanticExtension, Dialect)] =
    SemanticExtensionHelper.byPropertyTerm(configuration).find(uri).headOption

  /**
    * Find all instances of semantic extensions in the provided dialect filtering by the param
    * @param uri of the target field of the semantic extension to search
    * @return a Seq of [[SemanticExtension]]
    */
  def findSemanticByTarget(uri: String): Seq[(SemanticExtension, Dialect)] =
    SemanticExtensionHelper.byTargetFinder(configuration).find(uri)

  /**
    * Find all instances of semantic extensions in the provided dialect filtering by the param
    * @param name of the semantic extension to search
    * @return a Option of [[SemanticExtension]]
    */
  def findSemanticByName(name: String): Option[(SemanticExtension, Dialect)] =
    SemanticExtensionHelper.byNameFinder(configuration).find(name).headOption

  def findDialectFor(dialectInstance: DialectInstance): Option[Dialect] = {
    @silent("deprecated") // Silent can only be used in assignment expressions
    val a = getDialects().find(
        dialect =>
          dialectInstance.processingData
            .definedBy()
            .option()
            .orElse(dialectInstance.definedBy().option())
            .contains(dialect.id))
    a
  }

  private def getDialectsByCondition(filter: AMLDialectInstanceParsingPlugin => Boolean): immutable.Seq[Dialect] =
    configuration.registry.getPluginsRegistry.parsePlugins.collect {
      case plugin: AMLDialectInstanceParsingPlugin if filter(plugin) => plugin.dialect
    }

  private def dialectNameFilter(name: String): AMLDialectInstanceParsingPlugin => Boolean =
    (plugin: AMLDialectInstanceParsingPlugin) => plugin.dialect.hasValidHeader && plugin.dialect.name().value() == name

  private def dialectNameAndVersionFilter(name: String, version: String): AMLDialectInstanceParsingPlugin => Boolean =
    (plugin: AMLDialectInstanceParsingPlugin) =>
      plugin.dialect.hasValidHeader &&
        plugin.dialect.name().value() == name &&
        plugin.dialect.version().value() == version

}
