package amf.aml.client.scala

import amf.aml.client.scala.model.document.{Dialect, DialectInstance}
import amf.aml.client.scala.model.domain.SemanticExtension
import amf.aml.internal.parse.plugin.AMLDialectInstanceParsingPlugin
import amf.aml.internal.semantic.SemanticExtensionHelper

import scala.collection.immutable

/* Contains methods to get information about the current state of the configuration */
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
  def getExtensions(): immutable.Seq[SemanticExtension] = getDialects().flatMap(_.extensions())

  /**
    * Find all instances of semantic extensions in the registered dialects filtering by the param
    * @param uri of the propertyTerm of the semantic extension to search
    * @return a Map of [[Dialect]] to [[SemanticExtension]]
    */
  def findSemanticByPropertyTerm(uri: String): Map[Dialect, Seq[SemanticExtension]] =
    getDialects().map(d => d -> findSemanticByPropertyTerm(d, uri)).toMap

  /**
    * Find all instances of semantic extensions in the provided dialect filtering by the param
    * @param dialect where the semantic extension will be searched
    * @param uri of the propertyTerm of the semantic extension to search
    * @return a Seq of [[SemanticExtension]]
    */
  def findSemanticByPropertyTerm(dialect: Dialect, uri: String): Seq[SemanticExtension] =
    SemanticExtensionHelper.byPropertyTerm(dialect).find(uri)

  /**
    * Find all instances of semantic extensions in the registered dialects filtering by the param
    * @param uri of the target field of the semantic extension to search
    * @return a Map of [[Dialect]] to [[SemanticExtension]]
    */
  def findSemanticByTarget(uri: String): Map[Dialect, Seq[SemanticExtension]] =
    getDialects().map(d => d -> findSemanticByTarget(d, uri)).toMap

  /**
    * Find all instances of semantic extensions in the provided dialect filtering by the param
    * @param dialect where the semantic extension will be searched
    * @param uri of the target field of the semantic extension to search
    * @return a Seq of [[SemanticExtension]]
    */
  def findSemanticByTarget(dialect: Dialect, uri: String): Seq[SemanticExtension] =
    SemanticExtensionHelper.byTargetFinder(dialect).find(uri)

  /**
    * Find all instances of semantic extensions in the registered dialects filtering by the param
    * @param name of the semantic extension to search
    * @return a Option of a tuple of [[Dialect]] and [[SemanticExtension]]
    */
  def findSemanticByName(name: String): Option[(Dialect, SemanticExtension)] =
    getDialects().flatMap(d => findSemanticByName(d, name).map((d, _))).headOption

  /**
    * Find all instances of semantic extensions in the provided dialect filtering by the param
    * @param dialect where the semantic extension will be searched
    * @param name of the semantic extension to search
    * @return a Option of [[SemanticExtension]]
    */
  def findSemanticByName(dialect: Dialect, name: String): Option[SemanticExtension] =
    SemanticExtensionHelper.byNameFinder(dialect).find(name).headOption

  def findDialectFor(dialectInstance: DialectInstance): Option[Dialect] = {
    getDialects().find(dialect => dialectInstance.definedBy().value() == dialect.id)
  }

  private def getDialectsByCondition(filter: (AMLDialectInstanceParsingPlugin) => Boolean): immutable.Seq[Dialect] =
    configuration.registry.plugins.parsePlugins.collect {
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
